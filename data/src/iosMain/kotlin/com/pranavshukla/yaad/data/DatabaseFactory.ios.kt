package com.pranavshukla.yaad.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.pranavshukla.yaad.data.db.YaadDatabase

/** yaad.db in the app's Library folder. */
class DatabaseFactory {
    fun driver(): SqlDriver = NativeSqliteDriver(YaadDatabase.Schema, DatabaseName, onConfiguration = {
        it.copy(extendedConfig = it.extendedConfig.copy(foreignKeyConstraints = true))
    })
}
