package app.khom.pavlo.crypto.ui.common

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment

fun DialogFragment.applyAppDialogWindow(maxWidthDp: Int = 480) {
    val dialogWindow = dialog?.window ?: return
    val displayMetrics = resources.displayMetrics
    val maxWidth = (maxWidthDp * displayMetrics.density).toInt()
    val width = minOf((displayMetrics.widthPixels * DIALOG_WIDTH_RATIO).toInt(), maxWidth)
    dialogWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    dialogWindow.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    dialogWindow.attributes = dialogWindow.attributes.apply { dimAmount = DIALOG_DIM_AMOUNT }
    dialogWindow.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
}

private const val DIALOG_WIDTH_RATIO = 0.9f
private const val DIALOG_DIM_AMOUNT = 0.72f