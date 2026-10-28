package com.pranavshukla.yaad.android

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.pranavshukla.yaad.app.AlertAccess
import com.pranavshukla.yaad.app.App

class MainActivity : ComponentActivity() {
    private val notifications get() = (application as YaadApplication).notifications
    private val access = mutableStateOf(AlertAccess())

    private val askForNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshAccess()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { App((application as YaadApplication).reminders, access.value, ::fixAccess) }
        if (savedInstanceState == null && needsNotificationPermission()) {
            askForNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshAccess()
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
