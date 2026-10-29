package com.pranavshukla.yaad.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.runtime.remember
import com.pranavshukla.yaad.app.App
import com.pranavshukla.yaad.data.DatabaseFactory
import com.pranavshukla.yaad.data.LoanStore
import com.pranavshukla.yaad.data.ReminderStore
import com.pranavshukla.yaad.feature.loans.LoansModel
import com.pranavshukla.yaad.feature.reminders.RemindersModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Opens at iPad-portrait size; resize below 600 dp wide to see the phone layout. Desktop has no notifications. */
fun main() = application {
    val driver = remember { DatabaseFactory().driver() }
    val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
    val store = remember { ReminderStore(driver) }
    val reminders = remember { RemindersModel(store, scope) }
    val loans = remember { LoansModel(LoanStore(driver), store, scope) }
    Window(
        onCloseRequest = ::exitApplication,
        state = rememberWindowState(size = DpSize(820.dp, 1000.dp)),
        title = "Yaad",
    ) {
        window.minimumSize = java.awt.Dimension(360, 600)
        App(reminders, loans)
    }
}
