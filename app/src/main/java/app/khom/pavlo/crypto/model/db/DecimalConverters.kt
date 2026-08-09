package app.khom.pavlo.crypto.model.db

import androidx.room.TypeConverter
import java.math.BigDecimal

class DecimalConverters {

    @TypeConverter
    fun fromBigDecimal(value: BigDecimal): String = value.toPlainString()

    @TypeConverter
    fun toBigDecimal(value: String): BigDecimal = value.toBigDecimalOrNull() ?: BigDecimal.ZERO
}