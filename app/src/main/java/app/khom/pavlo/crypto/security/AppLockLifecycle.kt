package app.khom.pavlo.crypto.security

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import app.khom.pavlo.crypto.R

/**
 * Watches every screen of the app: counts how many are visible to tell when the app goes to the background and
 * comes back, and puts the lock screen in front of anything that opens while the app is locked.
 */
class AppLockLifecycle(private val appLock: AppLock) : Application.ActivityLifecycleCallbacks {

    private var started = 0

    override fun onActivityStarted(activity: Activity) {
        if (started++ == 0) appLock.onForegrounded()
        hideFromRecents(activity)
        if (activity is LockActivity) return
        if (appLock.mustAuthenticate) {
            cover(activity)
            activity.startActivity(LockActivity.intent(activity))
        }
    }

    override fun onActivityResumed(activity: Activity) {
        if (!appLock.mustAuthenticate) uncover(activity)
    }

    override fun onActivityStopped(activity: Activity) {
        if (--started == 0) appLock.onBackgrounded()
    }

    /**
     * The first frame of a screen can be drawn before the lock screen is shown, so a plain view hides it
     * until the user has authenticated.
     */
    private fun cover(activity: Activity) {
        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        if (content.findViewWithTag<View>(COVER_TAG) != null) return
        val background = TypedValue().also { activity.theme.resolveAttribute(android.R.attr.colorBackground, it, true) }
        val cover = View(activity).apply {
            tag = COVER_TAG
            // The wallpaper layer fades every fill it finds; this one has to stay fully opaque.
            setTag(R.id.wallpaper_keep_opaque, true)
            setBackgroundColor(background.data)
            isClickable = true
            elevation = COVER_ELEVATION
        }
        content.addView(cover, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    private fun uncover(activity: Activity) {
        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        content.findViewWithTag<View>(COVER_TAG)?.let(content::removeView)
    }

    /** The app switcher shows a picture of the last screen, which would show the portfolio. */
    private fun hideFromRecents(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.setRecentsScreenshotEnabled(!appLock.isEnabled)
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit

    private companion object {
        const val COVER_TAG = "app_lock_cover"
        const val COVER_ELEVATION = 10_000f
    }
}
