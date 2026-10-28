package com.pranavshukla.yaad.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.pranavshukla.yaad.data.db.YaadDatabase
import java.io.File
import java.util.Properties

/** yaad.db in a development folder (~/Library/Application Support/Yaad on macOS). */
class DatabaseFactory(private val folder: File = defaultFolder()) {
    fun driver(): SqlDriver {
        folder.mkdirs()
        val file = File(folder, DatabaseName)
        val isNew = !file.exists()
        val driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}", Properties().apply { put("foreign_keys", "true") })
        migrate(driver, isNew)
        return driver
    }

    companion object {
        /** Creates or migrates the schema, keeping the version in SQLite's user_version as the mobile drivers do. */
        fun migrate(driver: SqlDriver, isNew: Boolean) {
            val target = YaadDatabase.Schema.version
            if (isNew) {
                YaadDatabase.Schema.create(driver)
            } else {
                val current = driver.executeQuery(null, "PRAGMA user_version", { c ->
                    QueryResult.Value(if (c.next().value) c.getLong(0) ?: 0L else 0L)
                }, 0).value.coerceAtLeast(1L)
                if (current < target) YaadDatabase.Schema.migrate(driver, current, target)
            }
            driver.execute(null, "PRAGMA user_version = $target", 0)
        }

        fun defaultFolder(): File {
            val home = System.getProperty("user.home")
            val mac = File(home, "Library/Application Support")
            return if (mac.isDirectory) File(mac, "Yaad") else File(home, ".yaad")
        }
    }
}
