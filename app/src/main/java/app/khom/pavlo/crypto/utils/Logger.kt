package app.khom.pavlo.crypto.utils

import android.content.Context

class Logger(private val context: Context) {

    fun logDebug(message: String) = context.logDebug(message)

    fun logError(message: String) = context.logError(message)

}