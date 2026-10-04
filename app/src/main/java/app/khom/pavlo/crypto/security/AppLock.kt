package app.khom.pavlo.crypto.security

import android.os.SystemClock

/**
 * Decides when the app asks the user to authenticate again.
 *
 * A new process starts locked. Once unlocked, the app stays open while it is used and for [graceMillis] after it
 * leaves the screen, so a trip to a file picker, another app or a screen rotation doesn't ask again. Longer than
 * that, the next return locks it.
 */
class AppLock(
    private val store: Store,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
    private val graceMillis: Long = DEFAULT_GRACE_MILLIS
) {

    /** Where the on/off choice is kept. */
    interface Store {
        var enabled: Boolean
    }

    @Volatile
    private var locked: Boolean = store.enabled
    private var backgroundedAt: Long = clock()

    val isEnabled: Boolean get() = store.enabled

    /** True when the user has to authenticate before the app's content may be shown. */
    val mustAuthenticate: Boolean get() = locked && store.enabled

    /** The last screen of the app went away. */
    fun onBackgrounded() {
        backgroundedAt = clock()
    }

    /** A screen of the app came back after none was visible. */
    fun onForegrounded() {
        if (!locked && store.enabled && clock() - backgroundedAt >= graceMillis) locked = true
    }

    fun onUnlocked() {
        locked = false
    }

    /** Turns the lock on. The user has just authenticated, so the app stays open. */
    fun enable() {
        store.enabled = true
        locked = false
    }

    fun disable() {
        store.enabled = false
        locked = false
    }

    companion object {
        const val DEFAULT_GRACE_MILLIS = 30_000L
    }
}
