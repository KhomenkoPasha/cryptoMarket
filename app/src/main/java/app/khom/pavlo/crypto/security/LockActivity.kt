package app.khom.pavlo.crypto.security

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.biometric.BiometricPrompt
import app.khom.pavlo.crypto.R
import app.khom.pavlo.crypto.activities.BaseActivity
import app.khom.pavlo.crypto.databinding.ActivityLockBinding
import app.khom.pavlo.crypto.utils.toastShort
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Shown over the app until the user authenticates. Going back leaves the app instead of revealing it. */
@AndroidEntryPoint
class LockActivity : BaseActivity() {

    @Inject lateinit var appLock: AppLock
    private lateinit var binding: ActivityLockBinding
    private lateinit var prompt: BiometricPrompt

    /** Ask automatically when the screen appears, but not again after the user dismissed the dialog. */
    private var promptOnResume = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLockBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prompt = BiometricAuth.createPrompt(this, onSuccess = ::unlock, onFailure = {})
        binding.lockUnlock.setOnClickListener { authenticate() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                moveTaskToBack(true)
            }
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (!appLock.mustAuthenticate) {
            finish()
        } else if (promptOnResume) {
            promptOnResume = false
            authenticate()
        }
    }

    override fun onStop() {
        // Coming back from the home screen should ask again.
        promptOnResume = true
        super.onStop()
    }

    private fun authenticate() {
        when (BiometricAuth.availability(this)) {
            BiometricAuth.Availability.AVAILABLE ->
                BiometricAuth.show(prompt, getString(R.string.app_lock_prompt_title), getString(R.string.app_lock_prompt_subtitle))
            BiometricAuth.Availability.NOT_SET_UP -> {
                // The screen lock was removed after the app lock was turned on. Staying locked would lock the owner out.
                appLock.disable()
                toastShort(getString(R.string.app_lock_removed))
                finish()
            }
            BiometricAuth.Availability.TEMPORARILY_UNAVAILABLE -> toastShort(getString(R.string.app_lock_unavailable_now))
        }
    }

    private fun unlock() {
        appLock.onUnlocked()
        finish()
    }

    companion object {
        fun intent(context: Context): Intent = Intent(context, LockActivity::class.java)
    }
}
