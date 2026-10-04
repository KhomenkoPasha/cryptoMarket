package app.khom.pavlo.crypto.utils

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import app.khom.pavlo.crypto.R

/** Classic five-band reading of the 0-100 fear & greed index. */
@StringRes
fun fearGreedLabel(value: Int): Int = when {
    value < 25 -> R.string.fng_extreme_fear
    value < 45 -> R.string.fng_fear
    value < 56 -> R.string.fng_neutral
    value < 76 -> R.string.fng_greed
    else -> R.string.fng_extreme_greed
}

@ColorRes
fun fearGreedColor(value: Int): Int = when {
    value < 25 -> R.color.negative
    value < 45 -> R.color.warning
    value < 56 -> R.color.on_surface_variant
    else -> R.color.positive
}
