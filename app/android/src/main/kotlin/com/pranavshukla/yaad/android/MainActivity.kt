package com.pranavshukla.yaad.android

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.pranavshukla.yaad.app.AlertAccess
import com.pranavshukla.yaad.app.App
import com.pranavshukla.yaad.feature.settings.SettingsCapabilities
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/** A FragmentActivity because the biometric prompt needs one. */
class MainActivity : FragmentActivity() {
    private val notifications get() = (application as YaadApplication).notifications
    private val access = mutableStateOf(AlertAccess())
    private val locked = mutableStateOf(false)
    private var lockOn = false

    private val askForNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshAccess()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as YaadApplication
        lifecycleScope.launch {
            app.preferences.settings().collect { settings ->
                lockOn = settings.appLock
                if (!lockOn) locked.value = false
            }
        }
        setContent {
            App(
                app.reminders, app.loans, app.preferences, access.value, ::fixAccess,
                capabilities = SettingsCapabilities(canLock = canLock()),
                onSettingsChanged = { app.notifications.requestSync() },
                locked = locked.value,
                onUnlock = ::unlock,
            )
        }
        if (savedInstanceState == null && needsNotificationPermission()) {
            askForNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onStart() {
        super.onStart()
        val settings = runBlocking { (application as YaadApplication).preferences.settings().first() }
        lockOn = settings.appLock
        if (lockOn) {
            locked.value = true
            unlock()
        }
    }

    override fun onStop() {
        super.onStop()
        if (lockOn) locked.value = true
    }

    override fun onResume() {
        super.onResume()
        refreshAccess()
    }

    private fun canLock() = BiometricManager.from(this).canAuthenticate(Authenticators) == BiometricManager.BIOMETRIC_SUCCESS

    /** Fingerprint, face or the device PIN, whichever the user has set up. */
    private fun unlock() {
        if (!canLock()) {
            // The screen lock was removed since the app lock was turned on: nothing can verify the user.
            locked.value = false
            return
        }
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                locked.value = false
            }
        })
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.unlock_title))
                .setAllowedAuthenticators(Authenticators)
                .build(),
        )
    }

    private companion object {
        const val Authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }

    private fun refreshAccess() {
        access.value = AlertAccess(notifications.notificationsAllowed, notifications.exactAlarmsAllowed)
    }

    private fun needsNotificationPermission() =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notifications.notificationsAllowed

    /** Asks again where Android allows it, otherwise opens the right settings page. */
    private fun fixAccess() {
        when {
            !access.value.notifications && needsNotificationPermission() &&
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) ->
                askForNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            !access.value.notifications -> startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:$packageName".toUri()),
            )
        }
    }
}
