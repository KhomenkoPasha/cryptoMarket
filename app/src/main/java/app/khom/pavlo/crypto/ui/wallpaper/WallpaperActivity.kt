package app.khom.pavlo.crypto.ui.wallpaper

import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.GridLayout
import android.widget.ImageView
import androidx.core.view.ViewCompat
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.activities.BaseActivity
import app.khom.pavlo.crypto.databinding.ActivityWallpaperBinding
import app.khom.pavlo.crypto.databinding.ItemWallpaperTileBinding
import com.google.android.material.card.MaterialCardView
import com.squareup.picasso.Picasso
import kotlin.math.roundToInt

class WallpaperActivity : BaseActivity() {
    private lateinit var binding: ActivityWallpaperBinding
    private val tiles = mutableListOf<Pair<WallpaperOption?, MaterialCardView>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWallpaperBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.wallpaper_title)
        binding.toolbar.setNavigationOnClickListener { finish() }

        addTile(binding.wallpaperGrid, null, 0)
        WallpaperSettings.options.forEachIndexed { index, option -> addTile(binding.wallpaperGrid, option, index + 1) }
        refreshSelection()

        binding.wallpaperTransparency.addOnChangeListener { _, value, fromUser ->
            updateLabels()
            if (fromUser) WallpaperSettings.setTransparency(this, value.roundToInt())
        }
        binding.wallpaperDimming.addOnChangeListener { _, value, fromUser ->
            updateLabels()
            if (fromUser) WallpaperSettings.setDimming(this, value.roundToInt())
        }
    }

    /** Inflates one tile and places it in the grid; the row and column come from its position. */
    private fun addTile(
        grid: GridLayout,
        option: WallpaperOption?,
        index: Int,
    ) {
        val tile = ItemWallpaperTileBinding.inflate(layoutInflater, grid, false)
        val card = tile.root
        val title =
            if (option == null) {
                getString(R.string.wallpaper_none)
            } else {
                getString(R.string.wallpaper_number, option.number)
            }
        card.contentDescription = title
        tile.wallpaperTileTitle.text = title
        (card.layoutParams as GridLayout.LayoutParams).apply {
            rowSpec = GridLayout.spec(index / GRID_COLUMNS)
            columnSpec = GridLayout.spec(index % GRID_COLUMNS, 1f)
        }
        bindPreview(tile.wallpaperTilePreview, option)
        card.setOnClickListener {
            WallpaperSettings.select(this, option)
            refreshSelection()
        }
        tiles += option to card
        grid.addView(card)
    }

    /** The "no wallpaper" tile shows an icon; the rest show the photograph itself. */
    private fun bindPreview(
        preview: ImageView,
        option: WallpaperOption?,
    ) {
        // The backdrop keeps this one opaque while everything else is tinted through.
        preview.setTag(R.id.wallpaper_keep_opaque, true)
        if (option == null) {
            preview.scaleType = ImageView.ScaleType.CENTER
            preview.setImageResource(R.drawable.ic_wallpaper_24)
            preview.imageTintList = ColorStateList.valueOf(getColor(R.color.brand_primary))
            return
        }
        Picasso
            .get()
            .load(option.assetUri)
            .resize(PREVIEW_SIZE_PX, PREVIEW_SIZE_PX)
            .centerCrop()
            .into(preview)
    }

    private fun refreshSelection() {
        val appearance = WallpaperSettings.read(this)
        tiles.forEach { (option, card) ->
            val selected = option == appearance.wallpaper
            card.isChecked = selected
            card.strokeWidth = dp(if (selected) SELECTED_STROKE_DP else UNSELECTED_STROKE_DP)
            card.strokeColor = getColor(if (selected) R.color.brand_primary else R.color.glass_outline)
            ViewCompat.setStateDescription(card, if (selected) getString(R.string.wallpaper_selected) else null)
        }
        binding.wallpaperTransparency.value = appearance.transparency.toFloat()
        binding.wallpaperDimming.value = appearance.dimming.toFloat()
        binding.wallpaperTransparency.isEnabled = appearance.wallpaper != null
        binding.wallpaperDimming.isEnabled = appearance.wallpaper != null
        updateLabels()
    }

    private fun updateLabels() {
        binding.wallpaperTransparencyLabel.text =
            getString(R.string.wallpaper_transparency_value, binding.wallpaperTransparency.value.roundToInt())
        binding.wallpaperDimmingLabel.text =
            getString(R.string.wallpaper_dimming_value, binding.wallpaperDimming.value.roundToInt())
        binding.wallpaperTransparency.contentDescription = binding.wallpaperTransparencyLabel.text
        binding.wallpaperDimming.contentDescription = binding.wallpaperDimmingLabel.text
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    private companion object {
        const val GRID_COLUMNS = 2

        /** Wider than a tile on any density this app ships to, and half the asset. */
        const val PREVIEW_SIZE_PX = 480

        const val SELECTED_STROKE_DP = 2
        const val UNSELECTED_STROKE_DP = 1
    }
}
