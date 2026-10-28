package com.pranavshukla.yaad.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.wrapConnection
import co.touchlab.sqliter.DatabaseConfiguration
import com.pranavshukla.yaad.data.db.YaadDatabase

private var counter = 0

actual fun testDriver(): SqlDriver {
    val schema = YaadDatabase.Schema
    return NativeSqliteDriver(
        DatabaseConfiguration(
            name = "test-${counter++}",
            inMemory = true,
            version = schema.version.toInt(),
            create = { connection -> wrapConnection(connection) { schema.create(it) } },
            extendedConfig = DatabaseConfiguration.Extended(foreignKeyConstraints = true),
        ),
    )
}
