package com.pranavshukla.yaad.app

import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.model.Schedule
import com.pranavshukla.yaad.data.Backup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

/** Phase 10: an encrypted backup made on one device restores on a fresh one, and only with its password. */
class BackupFlowTest {
    private class FileStandIn : BackupHost {
        var name: String? = null
        var saved: String? = null
        override fun save(fileName: String, content: String) { name = fileName; saved = content }
        override fun open(onOpened: (String) -> Unit) = onOpened(saved!!)
    }

    @Test
    fun exportThenRestoreOnANewDevice() = runBlocking {
        val files = FileStandIn()
        val old = TestApp()
        old.store.save(Reminder(old.reminders.newId(), "Renew passport", Schedule(LocalDateTime(2027, 3, 12, 0, 0))))
        BackupFlow(Backup(old.driver), files).export("correct horse", Instant.parse("2026-10-29T10:00:00Z"))
        assertEquals("yaad-backup-2026-10-29.yaad", files.name)
        assertTrue("Renew passport" !in files.saved!!, "the file is encrypted")

        val new = TestApp()
        val flow = BackupFlow(Backup(new.driver), files)
        assertTrue(flow.restore(files.saved!!, "wrong horse").isFailure)
        assertEquals(emptyList(), new.store.reminders().first(), "a wrong password changes nothing")
        assertTrue(flow.restore(files.saved!!, "correct horse").isSuccess)
        assertEquals(listOf("Renew passport"), new.store.reminders().first().map { it.title })
    }
}
