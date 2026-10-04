package app.khom.pavlo.crypto.ui.wallpaper

import android.app.Activity
import android.app.Application
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.Window
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.graphics.ColorUtils
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.security.LockActivity
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputLayout
import com.squareup.picasso.Picasso
import java.util.WeakHashMap
import kotlin.math.max
import kotlin.math.roundToInt

/** One backdrop per activity. Never change a content view's alpha or IME/inset handling. */
internal class WallpaperLifecycleCallbacks(
    application: Application,
) : Application.ActivityLifecycleCallbacks {
    private val bindings = mutableMapOf<Activity, ActivityWallpaper>()
    private val preferences = WallpaperSettings.preferences(application)
    private val preferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            WallpaperSettings.invalidate()
            bindings.values.toList().forEach { it.update() }
        }

    init {
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
    }

    override fun onActivityResumed(activity: Activity) {
        if (!owns(activity)) return
        // ActivityLifecycleCallbacks.onActivityCreated runs inside super.onCreate,
        // before subclasses install their content. Attach only after creation finishes.
        bindings.getOrPut(activity) { ActivityWallpaper(activity) }.update()
    }

    override fun onActivityDestroyed(activity: Activity) {
        bindings.remove(activity)?.dispose()
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle,
    ) = Unit

    // Screens of this app only: ad and library activities keep their own look, and the lock
    // screen has to stay opaque.
    private fun owns(activity: Activity): Boolean =
        activity.javaClass.name.startsWith(APP_PACKAGE_PREFIX) && activity !is LockActivity

    private companion object {
        const val APP_PACKAGE_PREFIX = "app.khom.pavlo.crypto."
    }
}

private class ActivityWallpaper(
    private val activity: Activity,
) {
    private val decor = activity.window.decorView
    private val originalWindowBackground = decor.background
    private val backdropHost =
        decor as? ViewGroup
            ?: activity.findViewById<ViewGroup>(android.R.id.content)
    private val systemBars = WallpaperSystemBars(activity.window)
    private val picasso = Picasso.get()
    private var layer: FrameLayout? = null
    private var image: ImageView? = null
    private var veil: View? = null
    private var loadedImage: String? = null
    private var loadedSize: Pair<Int, Int>? = null
    private var wallpaperWindowBackground = false
    private var surfaceStyler: SurfaceStyler? = null
    private val layoutListener = ViewTreeObserver.OnGlobalLayoutListener { updateImageSize() }

    init {
        decor.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }

    fun update() {
        if (activity.isDestroyed || activity.isFinishing) return
        val appearance = WallpaperSettings.read(activity)
        if (appearance.wallpaper == null) {
            removeLayer()
            surfaceStyler?.dispose(restore = true)
            surfaceStyler = null
            systemBars.restore()
            return
        }
        systemBars.apply()
        ensureLayer()
        veil?.setBackgroundColor(
            ColorUtils.setAlphaComponent(activity.getColor(R.color.app_background), appearance.dimmingAlpha),
        )
        updateImageSize()
        if (surfaceStyler == null) {
            surfaceStyler = SurfaceStyler(decor, activity)
        }
        surfaceStyler?.apply()
    }

    private fun ensureLayer() {
        if (layer != null) return
        // Keep the window itself opaque. Only its content surfaces are translucent;
        // otherwise scrolling can leave pixels from previous frames on the surface.
        activity.window.setBackgroundDrawableResource(R.color.app_background)
        wallpaperWindowBackground = true
        val backdrop =
            FrameLayout(activity).apply {
                id = R.id.wallpaper_background_layer
                setTag(R.id.wallpaper_keep_opaque, true)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                isClickable = false
                isFocusable = false
                setBackgroundColor(activity.getColor(R.color.app_background))
            }
        image =
            ImageView(activity).also {
                it.scaleType = ImageView.ScaleType.CENTER_CROP
                backdrop.addView(it, FrameLayout.LayoutParams(-1, -1))
            }
        veil = View(activity).also { backdrop.addView(it, FrameLayout.LayoutParams(-1, -1)) }
        // Draw behind the entire window, including status/navigation bars, without
        // changing the padding or IME listener installed on the activity's content.
        backdropHost.addView(backdrop, 0, ViewGroup.LayoutParams(-1, -1))
        layer = backdrop
    }

    @Suppress("ReturnCount")
    private fun updateImageSize() {
        val target = image ?: return
        val option = WallpaperSettings.read(activity).wallpaper ?: return
        val metrics = activity.resources.displayMetrics
        val width = backdropHost.width.takeIf { it > 0 } ?: metrics.widthPixels
        val height = backdropHost.height.takeIf { it > 0 } ?: metrics.heightPixels
        // Bound decoded memory even for the original full-resolution bundled photographs.
        val scale = minOf(1f, MAX_DECODED_EDGE / max(width, height).coerceAtLeast(1))
        val size =
            (width * scale).roundToInt().coerceAtLeast(1) to
                (height * scale).roundToInt().coerceAtLeast(1)
        if (loadedImage == option.fileName && loadedSize == size) return
        loadedImage = option.fileName
        loadedSize = size
        picasso
            .load(option.assetUri)
            .resize(size.first, size.second)
            .centerCrop()
            .noFade()
            .into(target)
    }

    private fun removeLayer() {
        image?.let { picasso.cancelRequest(it) }
        layer?.let { (it.parent as? ViewGroup)?.removeView(it) }
        layer = null
        image = null
        veil = null
        loadedImage = null
        loadedSize = null
        if (wallpaperWindowBackground) {
            activity.window.setBackgroundDrawable(originalWindowBackground)
            wallpaperWindowBackground = false
        }
    }

    fun dispose() {
        if (decor.viewTreeObserver.isAlive) {
            decor.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
        }
        surfaceStyler?.dispose(restore = false)
        removeLayer()
    }

    private companion object {
        const val MAX_DECODED_EDGE = 1920f
    }
}

/** Apply translucency to fills, not text, media, or interactive drawing contents. */
private class SurfaceStyler(
    private val root: View,
    private val activity: Activity,
) {
    private val backgrounds = WeakHashMap<View, BackgroundState>()
    private val cards = WeakHashMap<CardView, CardState>()
    private val fields = WeakHashMap<TextInputLayout, FieldState>()
    private var applying = false
    private val handler = Handler(Looper.getMainLooper())
    private var applyScheduled = false
    private val applyRunnable =
        Runnable {
            applyScheduled = false
            apply()
        }

    // Scrolling produces a layout pass per frame, and each one used to walk the whole
    // tree again. Collapse every pass in the same message-queue turn into a single walk.
    private val layoutListener = ViewTreeObserver.OnGlobalLayoutListener { scheduleApply() }

    init {
        root.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }

    private fun scheduleApply() {
        if (applyScheduled) return
        applyScheduled = true
        handler.post(applyRunnable)
    }

    fun apply() {
        if (applying) return
        val appearance = WallpaperSettings.read(activity)
        if (appearance.wallpaper == null) return
        applying = true
        try {
            visit(root, appearance.surfaceOpacity)
        } finally {
            applying = false
        }
    }

    // Classifying a view against every surface kind the app uses is inherently branchy.
    // Splitting it would scatter a single decision across several functions.
    @Suppress("CyclomaticComplexMethod", "ComplexCondition")
    private fun visit(
        view: View,
        opacity: Float,
    ) {
        // Chips own their translucency and contrast; do not fade them twice.
        if (view is Chip) return
        if (view.getTag(R.id.wallpaper_keep_opaque) == true ||
            view is ImageView ||
            view is SurfaceView ||
            view is TextureView ||
            view is WebView ||
            view.javaClass.name.startsWith(ADS_PACKAGE_PREFIX)
        ) {
            return
        }

        if (view is CardView) {
            val state = cards.getOrPut(view) { CardState(view.cardBackgroundColor) }
            if (view.cardBackgroundColor !== state.applied && view.cardBackgroundColor !== state.original) {
                state.original = view.cardBackgroundColor
            }
            val alpha = (Color.alpha(state.original.defaultColor) * opacity).roundToInt()
            if (state.applied == null || Color.alpha(view.cardBackgroundColor.defaultColor) != alpha) {
                state.applied = state.original.withAlpha(alpha)
                view.setCardBackgroundColor(state.applied)
            }
        } else {
            styleBackground(view, opacity)
        }
        if (view is TextInputLayout) {
            val state = fields.getOrPut(view) { FieldState(view.boxBackgroundColor) }
            if (view.boxBackgroundColor != state.applied && view.boxBackgroundColor != state.original) {
                state.original = view.boxBackgroundColor
            }
            val color =
                ColorUtils.setAlphaComponent(
                    state.original,
                    (Color.alpha(state.original) * opacity).roundToInt(),
                )
            if (view.boxBackgroundColor != color) view.boxBackgroundColor = color
            state.applied = color
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) visit(view.getChildAt(index), opacity)
        }
    }

    // Same reasoning as visit(): the early returns and the container test are one
    // classification, and the guards read better inline than extracted.
    @Suppress("CyclomaticComplexMethod", "ComplexCondition", "ReturnCount")
    private fun styleBackground(
        view: View,
        opacity: Float,
    ) {
        // DecorView owns the opaque clearing background beneath our wallpaper layer.
        if (view === root) return
        val drawable = view.background ?: return
        if (drawable is BitmapDrawable) return
        val state =
            backgrounds[view]?.takeIf { it.drawable === drawable }
                ?: BackgroundState(drawable, drawable.alpha).also { backgrounds[view] = it }
        val screenContainer =
            view is ViewGroup &&
                view !is Toolbar &&
                view !is AppBarLayout &&
                view !is TextInputLayout &&
                !view.isClickable &&
                root.width > 0 &&
                root.height > 0 &&
                view.width >= root.width * SCREEN_WIDTH_SHARE &&
                view.height >= root.height * SCREEN_HEIGHT_SHARE
        val headerOrSystemBar =
            view is Toolbar ||
                view is AppBarLayout ||
                view.id == android.R.id.statusBarBackground ||
                view.id == android.R.id.navigationBarBackground
        val alpha =
            if (screenContainer || headerOrSystemBar) {
                0
            } else {
                (state.originalAlpha * opacity).roundToInt()
            }
        if (drawable.alpha != alpha) drawable.mutate().alpha = alpha
    }

    fun dispose(restore: Boolean) {
        handler.removeCallbacks(applyRunnable)
        applyScheduled = false
        if (root.viewTreeObserver.isAlive) root.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
        if (restore) {
            backgrounds.forEach { (view, state) ->
                if (view.background === state.drawable) state.drawable.alpha = state.originalAlpha
            }
            cards.forEach { (view, state) ->
                if (view.cardBackgroundColor === state.applied) view.setCardBackgroundColor(state.original)
            }
            fields.forEach { (view, state) ->
                if (view.boxBackgroundColor == state.applied) view.boxBackgroundColor = state.original
            }
        }
        backgrounds.clear()
        cards.clear()
        fields.clear()
    }

    private data class BackgroundState(
        val drawable: Drawable,
        val originalAlpha: Int,
    )

    private data class CardState(
        var original: ColorStateList,
        var applied: ColorStateList? = null,
    )

    private data class FieldState(
        var original: Int,
        var applied: Int? = null,
    )

    private companion object {
        const val ADS_PACKAGE_PREFIX = "com.google.android.gms.ads."

        // A view this large is the screen's own background, not a panel on it.
        const val SCREEN_WIDTH_SHARE = 0.9f
        const val SCREEN_HEIGHT_SHARE = 0.65f
    }
}

/** Preserve the normal theme when wallpapers are turned off. */
@Suppress("DEPRECATION")
private class WallpaperSystemBars(
    private val window: Window,
) {
    private val originalStatusColor = window.statusBarColor
    private val originalNavigationColor = window.navigationBarColor
    private val originalDividerColor =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) window.navigationBarDividerColor else Color.TRANSPARENT
    private val originalStatusContrast =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && window.isStatusBarContrastEnforced
    private val originalNavigationContrast =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && window.isNavigationBarContrastEnforced
    private var applied = false

    fun apply() {
        // Android 15+ manages edge-to-edge bar colors; older versions need explicit transparency.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.navigationBarDividerColor = Color.TRANSPARENT
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        applied = true
    }

    fun restore() {
        if (!applied) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            window.statusBarColor = originalStatusColor
            window.navigationBarColor = originalNavigationColor
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.navigationBarDividerColor = originalDividerColor
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = originalStatusContrast
            window.isNavigationBarContrastEnforced = originalNavigationContrast
        }
        applied = false
    }
}
