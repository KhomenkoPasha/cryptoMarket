package app.khom.pavlo.crypto.model

/**
 * Stores an ordered list of arbitrary note strings without reserving any separator character.
 * The length-prefixed format keeps multiline and Unicode notes intact.
 */
internal object NoteListCodec {

    private const val PREFIX = "note-list-v1|"

    fun encode(notes: List<String>): String = buildString {
        append(PREFIX)
        notes.forEach { note ->
            append(note.length)
            append(':')
            append(note)
        }
    }

    fun decode(value: String): List<String> {
        if (!value.startsWith(PREFIX)) return emptyList()

        val notes = mutableListOf<String>()
        var position = PREFIX.length
        while (position < value.length) {
            val separator = value.indexOf(':', position)
            if (separator < 0) return emptyList()

            val length = value.substring(position, separator).toIntOrNull()
                ?: return emptyList()
            val noteStart = separator + 1
            if (length < 0 || length > value.length - noteStart) return emptyList()

            val noteEnd = noteStart + length
            notes += value.substring(noteStart, noteEnd)
            position = noteEnd
        }
        return notes
    }
}