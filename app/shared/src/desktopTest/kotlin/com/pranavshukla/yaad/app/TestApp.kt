package com.pranavshukla.yaad.app

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.pranavshukla.yaad.data.LoanStore
import com.pranavshukla.yaad.data.PreferenceStore
import com.pranavshukla.yaad.data.ReminderStore
import com.pranavshukla.yaad.feature.loans.LoansModel
import com.pranavshukla.yaad.data.db.YaadDatabase
import com.pranavshukla.yaad.feature.reminders.RemindersModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.Properties

/** A fresh in-memory database with the app's models, as on first launch. */
internal class TestApp(onChanged: (ReminderStore) -> Unit = {}) {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, Properties().apply { put("foreign_keys", "true") })
        .also { YaadDatabase.Schema.create(it) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val store = ReminderStore(driver)
    val reminders = RemindersModel(store, scope, onChanged = { onChanged(store) })
    val loans = LoansModel(LoanStore(driver), store, scope, onChanged = { onChanged(store) })
    val preferences = PreferenceStore(driver)
}
