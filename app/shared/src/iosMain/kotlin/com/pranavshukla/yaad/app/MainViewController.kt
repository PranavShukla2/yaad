package com.pranavshukla.yaad.app

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.ComposeUIViewController
import com.pranavshukla.yaad.feature.settings.SettingsCapabilities
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIViewController

/** Entry point for the iOS app (see app/ios/Yaad/YaadApp.swift). */
@Suppress("FunctionName", "unused")
fun MainViewController(): UIViewController = ComposeUIViewController {
    val allowed by YaadIos.notifications.notificationsAllowed.collectAsState()
    val locked by YaadIos.locked.collectAsState()
    App(
        YaadIos.reminders, YaadIos.loans, YaadIos.preferences, AlertAccess(notifications = allowed), ::openSettings,
        capabilities = SettingsCapabilities(canLock = YaadIos.canLock()),
        onSettingsChanged = { YaadIos.notifications.requestSync() },
        backup = YaadIos.backup,
        backupHost = IosBackupHost,
        locked = locked,
        onUnlock = YaadIos::unlock,
    )
}

/** iOS cannot ask twice: send the user to Yaad's page in Settings. */
private fun openSettings() {
    val url = NSURL(string = UIApplicationOpenSettingsURLString)
    UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
}
