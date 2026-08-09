package app.khom.pavlo.crypto.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import app.khom.pavlo.crypto.R

fun Context.toastShort(message: CharSequence) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Context.logDebug(message: String) {
    Log.d("CryptoMoon debug", message)
}

fun Context.logError(message: String) {
    Log.e("CryptoMoon error", message)
}

fun SwipeRefreshLayout.applyCryptoRefreshStyle() {
    setProgressBackgroundColorSchemeColor(
        ContextCompat.getColor(context, R.color.surface_container_highest)
    )
    setColorSchemeResources(
        R.color.brand_primary,
        R.color.brand_secondary,
        R.color.brand_tertiary
    )
}