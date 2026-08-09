package app.khom.pavlo.crypto.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SupportedLanguagesTest {

    @Test
    fun `thirteen unique languages are exposed`() {
        assertEquals(13, SupportedLanguages.all.size)
        assertEquals(13, SupportedLanguages.all.map { it.tag }.distinct().size)
    }

    @Test
    fun `regional system locales resolve to supported variants`() {
        assertEquals(SupportedLanguages.PORTUGUESE_BRAZIL, SupportedLanguages.normalize("pt-PT"))
        assertEquals(SupportedLanguages.CHINESE_SIMPLIFIED, SupportedLanguages.normalize("zh-Hans"))
        assertEquals(SupportedLanguages.SPANISH, SupportedLanguages.normalize("es-MX"))
    }

    @Test
    fun `every supported language has a native display name`() {
        SupportedLanguages.all.forEach { language ->
            assertNotNull(SupportedLanguages.nativeDisplayName(language.tag).takeIf { it.isNotBlank() })
        }
    }
}