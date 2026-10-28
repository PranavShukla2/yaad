package com.pranavshukla.yaad.feature.reminders

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.pranavshukla.yaad.data.ReminderStore
import com.pranavshukla.yaad.data.db.YaadDatabase
import java.util.Properties
import kotlin.coroutines.CoroutineContext
import kotlin.time.Clock
import kotlin.time.Instant

/** A fixed clock at Wednesday 28 October 2026, 15:30 IST. */
internal object TestClock : Clock {
    var now: Instant = Instant.parse("2026-10-28T10:00:00Z")
    override fun now() = now
}

internal fun testStore(context: CoroutineContext): ReminderStore {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, Properties().apply { put("foreign_keys", "true") })
    YaadDatabase.Schema.create(driver)
    return ReminderStore(driver, TestClock, context)
}
