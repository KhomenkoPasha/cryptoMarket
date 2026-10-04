package app.khom.pavlo.crypto.ui.portfolio

import android.content.Context
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.model.Portfolio

/** The name to show for a portfolio; the built-in one has no stored name. */
fun Portfolio.displayName(context: Context): String =
    name.ifBlank { context.getString(R.string.portfolio_default_name) }
