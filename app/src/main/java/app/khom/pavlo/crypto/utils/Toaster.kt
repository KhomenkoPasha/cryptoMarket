package app.khom.pavlo.crypto.utils

import android.content.Context

class Toaster(private val context: Context) {

    fun toastShort(message: String) = context.toastShort(message)

    fun toastLong(message: String) = context.toastLong(message)

}