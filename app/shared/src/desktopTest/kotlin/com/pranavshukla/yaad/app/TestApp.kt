package com.pranavshukla.yaad.app

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.pranavshukla.yaad.data.ReminderStore
import com.pranavshukla.yaad.data.db.YaadDatabase
import com.pranavshukla.yaad.feature.reminders.RemindersModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.Properties

/** A fresh in-memory database and model, as the app has on first launch. */
internal fun testModel(onChanged: (ReminderStore) -> Unit = {}): RemindersModel {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, Properties().apply { put("foreign_keys", "true") })
    YaadDatabase.Schema.create(driver)
    val store = ReminderStore(driver)
    return RemindersModel(store, CoroutineScope(SupervisorJob() + Dispatchers.Default), onChanged = { onChanged(store) })
}
