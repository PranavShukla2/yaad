package com.pranavshukla.yaad.app

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.ComposeUIViewController
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIViewController

/** Entry point for the iOS app (see app/ios/Yaad/YaadApp.swift). */
@Suppress("FunctionName", "unused")
fun MainViewController(): UIViewController = ComposeUIViewController {
    val allowed by YaadIos.notifications.notificationsAllowed.collectAsState()
    App(alertAccess = AlertAccess(notifications = allowed), onFixAlertAccess = ::openSettings)
}

/** iOS cannot ask twice: send the user to Yaad's page in Settings. */
private fun openSettings() {
    val url = NSURL(string = UIApplicationOpenSettingsURLString)
    UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
}
