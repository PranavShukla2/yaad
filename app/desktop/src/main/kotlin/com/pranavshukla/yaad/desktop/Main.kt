package com.pranavshukla.yaad.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.pranavshukla.yaad.app.App

/** Opens at iPad-portrait size; resize below 600 dp wide to see the phone layout. */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        state = rememberWindowState(size = DpSize(820.dp, 1000.dp)),
        title = "Yaad",
    ) {
        window.minimumSize = java.awt.Dimension(360, 600)
        App()
    }
}
