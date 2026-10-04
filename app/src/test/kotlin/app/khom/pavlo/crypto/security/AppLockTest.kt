package app.khom.pavlo.crypto.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockTest {

    private class MemoryStore(override var enabled: Boolean) : AppLock.Store

    private var now = 1_000_000L
    private fun lock(enabled: Boolean) = AppLock(MemoryStore(enabled), clock = { now }, graceMillis = GRACE)

    @Test
    fun `a new process starts locked when the lock is on`() {
        assertTrue(lock(enabled = true).mustAuthenticate)
    }

    @Test
    fun `nothing is ever asked when the lock is off`() {
        val lock = lock(enabled = false)

        lock.onBackgrounded()
        now += GRACE * 10
        lock.onForegrounded()

        assertFalse(lock.mustAuthenticate)
    }

    @Test
    fun `unlocking opens the app`() {
        val lock = lock(enabled = true)

        lock.onUnlocked()

        assertFalse(lock.mustAuthenticate)
    }

    @Test
    fun `coming back within the grace period keeps the app open`() {
        val lock = lock(enabled = true).apply { onUnlocked() }

        lock.onBackgrounded()
        now += GRACE - 1
        lock.onForegrounded()

        assertFalse(lock.mustAuthenticate)
    }

    @Test
    fun `coming back after the grace period locks the app`() {
        val lock = lock(enabled = true).apply { onUnlocked() }

        lock.onBackgrounded()
        now += GRACE
        lock.onForegrounded()

        assertTrue(lock.mustAuthenticate)
    }

    @Test
    fun `each trip to the background restarts the clock`() {
        val lock = lock(enabled = true).apply { onUnlocked() }

        repeat(5) {
            lock.onBackgrounded()
            now += GRACE - 1
            lock.onForegrounded()
        }

        assertFalse(lock.mustAuthenticate)
    }

    @Test
    fun `staying locked does not need another background trip`() {
        val lock = lock(enabled = true)

        lock.onBackgrounded()
        lock.onForegrounded()

        assertTrue(lock.mustAuthenticate)
    }

    @Test
    fun `turning the lock on keeps the current session open`() {
        val store = MemoryStore(enabled = false)
        val lock = AppLock(store, clock = { now }, graceMillis = GRACE)

        lock.enable()

        assertTrue(store.enabled)
        assertTrue(lock.isEnabled)
        assertFalse(lock.mustAuthenticate)
    }

    @Test
    fun `a lock turned on during the session applies after the next absence`() {
        val lock = lock(enabled = false)
        lock.enable()

        lock.onBackgrounded()
        now += GRACE
        lock.onForegrounded()

        assertTrue(lock.mustAuthenticate)
    }

    @Test
    fun `turning the lock off releases a locked app`() {
        val store = MemoryStore(enabled = true)
        val lock = AppLock(store, clock = { now }, graceMillis = GRACE)

        lock.disable()

        assertFalse(store.enabled)
        assertFalse(lock.mustAuthenticate)
    }

    private companion object {
        const val GRACE = 30_000L
    }
}
