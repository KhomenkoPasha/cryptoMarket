package app.khom.pavlo.crypto.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** The portfolio every transaction belonged to before portfolios existed. */
const val DEFAULT_PORTFOLIO_ID = 1L

/** Selection value meaning "show every portfolio together". */
const val ALL_PORTFOLIOS_ID = 0L

/**
 * A named group of transactions, such as an exchange account or a cold wallet. A blank [name]
 * means the built-in default portfolio, which is shown under a localized name.
 */
@Entity(tableName = "portfolios")
data class Portfolio(
        @PrimaryKey(autoGenerate = true) var id: Long = 0,
        var name: String = "",
        @ColumnInfo(name = "created_at") var createdAt: Long = 0L,
        @ColumnInfo(name = "sort_order") var sortOrder: Int = 0)
