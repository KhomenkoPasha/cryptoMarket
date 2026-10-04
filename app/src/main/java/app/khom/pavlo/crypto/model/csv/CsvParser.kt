package app.khom.pavlo.crypto.model.csv

/** A small RFC 4180 reader and writer, enough for exchange exports without another dependency. */
object CsvParser {

    /** Spreadsheets add this invisible character to the start of UTF-8 files. */
    internal val BYTE_ORDER_MARK: String = Char(0xFEFF).toString()

    /** Reads every non-blank row. The delimiter (comma, semicolon or tab) is guessed from the first line. */
    fun parse(text: String): List<List<String>> {
        val input = text.removePrefix(BYTE_ORDER_MARK)
        val delimiter = detectDelimiter(input)
        val rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val field = StringBuilder()
        var inQuotes = false

        fun endField() {
            row.add(field.toString())
            field.setLength(0)
        }

        fun endRow() {
            endField()
            if (row.size > 1 || row[0].isNotBlank()) rows.add(row)
            row = ArrayList()
        }

        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < input.length && input[i + 1] == '"') {
                        field.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    field.append(c)
                }
            } else {
                when {
                    c == '"' && field.isEmpty() -> inQuotes = true
                    c == delimiter -> endField()
                    c == '\n' -> endRow()
                    c == '\r' -> Unit
                    else -> field.append(c)
                }
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) endRow()
        return rows
    }

    private fun detectDelimiter(input: String): Char {
        val firstLine = input.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
        val candidates = charArrayOf(',', ';', '\t')
        return candidates.maxByOrNull { delimiter -> firstLine.count { it == delimiter } }
            ?.takeIf { delimiter -> firstLine.contains(delimiter) }
            ?: ','
    }

    /** Joins fields into one CSV line, quoting those that need it. */
    fun line(vararg fields: String): String = fields.joinToString(",") { quote(it) }

    fun quote(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }

    /**
     * Free text from the user can start with characters that a spreadsheet would run as a formula.
     * A leading apostrophe keeps it plain text.
     */
    fun safeText(text: String): String =
        if (text.isNotEmpty() && text[0] in "=+-@\t\r") "'$text" else text
}
