package com.pranavshukla.yaad.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.pranavshukla.yaad.core.model.ReminderId
import com.pranavshukla.yaad.data.db.YaadDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * Every schema version Yaad has shipped is kept in `resources/schemas/N.sql`, with sample rows in `N-data.sql`.
 * Changing Yaad.sq without adding a migration and a new snapshot fails here.
 */
class MigrationTest {
    private val current = YaadDatabase.Schema.version

    private fun resource(name: String): String? = javaClass.getResource("/schemas/$name")?.readText()

    private fun newDriver(url: String = JdbcSqliteDriver.IN_MEMORY) =
        JdbcSqliteDriver(url, Properties().apply { put("foreign_keys", "true") })

    private fun SqlDriver.run(script: String) =
        script.split(";\n").map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("--") }.forEach {
            execute(null, it, 0)
        }

    private fun SqlDriver.strings(sql: String): List<String> = executeQuery(null, sql, { c ->
        val out = mutableListOf<String>()
        while (c.next().value) out += (0 until 8).map { runCatching { c.getString(it) }.getOrNull() }.joinToString("|")
        QueryResult.Value(out)
    }, 0).value

    /** Tables with their columns, foreign keys and indexes: what the schema means, whatever SQL built it. */
    private fun SqlDriver.structure(): List<String> =
        strings("SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' ORDER BY name")
            .map { it.substringBefore('|') }
            .flatMap { table ->
                listOf("table $table") +
                    strings("PRAGMA table_info($table)").map { "  column $it" } +
                    strings("PRAGMA foreign_key_list($table)").map { "  foreign key $it" } +
                    strings("SELECT name, sql FROM sqlite_master WHERE type = 'index' AND tbl_name = '$table' ORDER BY name")
                        .map { "  index $it" }
            }

    private fun createStatements(driver: SqlDriver): String =
        driver.strings("SELECT sql FROM sqlite_master WHERE sql IS NOT NULL ORDER BY rowid")
            .joinToString("") { it.substringBefore('|') + ";\n" }

    @Test
    fun theCurrentSchemaHasASnapshot() {
        val fresh = newDriver().also { YaadDatabase.Schema.create(it) }
        val snapshot = resource("$current.sql") ?: run {
            val file = File("build/schemas/$current.sql").apply { parentFile.mkdirs(); writeText(createStatements(fresh)) }
            fail("No snapshot for schema version $current; review ${file.absolutePath} and add it to resources/schemas")
        }
        assertEquals(fresh.structure(), newDriver().apply { run(snapshot) }.structure())
    }

    @Test
    fun everyVersionMigratesToTheCurrentSchemaKeepingItsData() {
        val expected = newDriver().also { YaadDatabase.Schema.create(it) }.structure()
        for (version in 1L..current) {
            val driver = newDriver()
            driver.run(resource("$version.sql") ?: fail("Missing snapshot $version.sql"))
            driver.run(resource("$version-data.sql") ?: fail("Missing sample data $version-data.sql"))
            if (version < current) YaadDatabase.Schema.migrate(driver, version, current)
            assertEquals(expected, driver.structure(), "schema after migrating from version $version")
            assertSampleDataReadable(driver, "version $version")
        }
    }

    @Test
    fun anOlderFileIsMigratedWhenOpened() {
        val folder = Files.createTempDirectory("yaad").toFile()
        newDriver("jdbc:sqlite:${File(folder, DatabaseName)}").apply {
            run(resource("1.sql")!!)
            run(resource("1-data.sql")!!)
            execute(null, "PRAGMA user_version = 1", 0)
            close()
        }
        val driver = DatabaseFactory(folder).driver()
        assertEquals(listOf("$current"), driver.strings("PRAGMA user_version").map { it.substringBefore('|') })
        assertSampleDataReadable(driver, "opened file")
        driver.close()
        folder.deleteRecursively()
    }

    /** The sample rows every N-data.sql holds: two reminders, one with a checklist and a paid occurrence. */
    private fun assertSampleDataReadable(driver: SqlDriver, label: String) = runBlocking {
        val store = ReminderStore(driver)
        assertEquals(listOf("Call mom", "Home loan EMI"), store.reminders().first().map { it.title }, label)
        assertEquals(2, store.reminder(ReminderId("emi-1"))!!.checklist.size, label)
        assertEquals(1, store.occurrences(ReminderId("emi-1")).size, label)
    }
}
