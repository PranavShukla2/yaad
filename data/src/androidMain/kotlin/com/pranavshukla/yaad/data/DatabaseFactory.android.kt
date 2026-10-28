package com.pranavshukla.yaad.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.pranavshukla.yaad.data.db.YaadDatabase

/** yaad.db in the app's private data folder (kept out of Android backups by the manifest). */
class DatabaseFactory(private val context: Context) {
    fun driver(): SqlDriver = AndroidSqliteDriver(
        schema = YaadDatabase.Schema,
        context = context,
        name = DatabaseName,
        callback = object : AndroidSqliteDriver.Callback(YaadDatabase.Schema) {
            override fun onConfigure(db: SupportSQLiteDatabase) {
                db.setForeignKeyConstraintsEnabled(true)
            }
        },
    )
}
