package app.khom.pavlo.crypto.model

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteListCodecTest {

    @Test
    fun `empty list round trips`() {
        assertEquals(emptyList<String>(), NoteListCodec.decode(NoteListCodec.encode(emptyList())))
    }

    @Test
    fun `multiple arbitrary notes round trip in order`() {
        val notes = listOf(
                "First note",
                "Line one\nLine two: with separator",
                "Криптовалюта 🚀"
        )

        assertEquals(notes, NoteListCodec.decode(NoteListCodec.encode(notes)))
    }

    @Test
    fun `malformed value is safely ignored`() {
        assertEquals(emptyList<String>(), NoteListCodec.decode("note-list-v1|99:short"))
        assertEquals(emptyList<String>(), NoteListCodec.decode("legacy note"))
    }
}