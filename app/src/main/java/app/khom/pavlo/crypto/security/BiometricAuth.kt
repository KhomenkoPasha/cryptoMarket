package app.khom.pavlo.crypto.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** Fingerprint or face, with the device PIN, pattern or password as the fallback. */
object BiometricAuth {

    private const val AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    enum class Availability {
        AVAILABLE,

        /** The device has no screen lock and no biometrics, so there is nothing to authenticate with. */
        NOT_SET_UP,

        /** Authentication can't be used right now, for example after too many failed attempts. */
        TEMPORARILY_UNAVAILABLE
    }

    fun availability(activity: FragmentActivity): Availability =
        when (BiometricManager.from(activity).canAuthenticate(AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> Availability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED,
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> Availability.TEMPORARILY_UNAVAILABLE
            else -> Availability.NOT_SET_UP
        }

    /**
     * Must be created in `onCreate`, so the system dialog is re-attached after a rotation. [onSuccess] runs after the
     * user authenticated; [onFailure] after the dialog was closed without success.
     */
    fun createPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ): BiometricPrompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()

            // Wrong fingers are retried inside the dialog; an error means it was closed.
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onFailure()
        }
    )

    fun show(prompt: BiometricPrompt, title: String, subtitle: String) {
        // No negative button: the device credential is the alternative, which the dialog offers itself.
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()
        prompt.authenticate(info)
    }
}
