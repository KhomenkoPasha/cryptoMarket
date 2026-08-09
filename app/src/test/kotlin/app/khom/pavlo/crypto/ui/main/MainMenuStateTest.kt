package app.khom.pavlo.crypto.ui.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainMenuStateTest {

    @Test
    fun `favorite page exposes add coin and sort actions`() {
        val state = mainMenuState(page = 0, coinsSelected = false)

        assertEquals(MainAddAction.ADD_COIN, state.addAction)
        assertTrue(state.showAdd)
        assertTrue(state.showSort)
        assertFalse(state.showDelete)
    }

    @Test
    fun `favorite selection mode exposes only delete action`() {
        val state = mainMenuState(page = 0, coinsSelected = true)

        assertEquals(MainAddAction.NONE, state.addAction)
        assertFalse(state.showAdd)
        assertFalse(state.showSort)
        assertTrue(state.showDelete)
        assertFalse(state.showOverflow)
    }

    @Test
    fun `portfolio page exposes add transaction action`() {
        val state = mainMenuState(page = 1, coinsSelected = false)

        assertEquals(MainAddAction.ADD_TRANSACTION, state.addAction)
        assertTrue(state.showAdd)
        assertFalse(state.showSort)
        assertFalse(state.showDelete)
    }

    @Test
    fun `other pages hide add action`() {
        listOf(2, 3, 4).forEach { page ->
            val state = mainMenuState(page, coinsSelected = false)
            assertEquals(MainAddAction.NONE, state.addAction)
            assertFalse(state.showAdd)
        }
    }
}