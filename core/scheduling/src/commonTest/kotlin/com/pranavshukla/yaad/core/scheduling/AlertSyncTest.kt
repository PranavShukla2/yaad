package com.pranavshukla.yaad.core.scheduling

import com.pranavshukla.yaad.core.model.ReminderId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class AlertSyncTest {
    private val due = Instant.parse("2026-10-29T03:30:00Z")
    private val early = Alert(ReminderId("a"), due, due - 1.hours, AlertKind.Early, 1.hours)
    private val onTime = Alert(ReminderId("a"), due, due, AlertKind.Due)
    private val other = Alert(ReminderId("b"), due, due, AlertKind.Due)

    @Test
    fun schedulesWhatIsMissingAndCancelsWhatIsStale() {
        val sync = AlertSync.diff(scheduled = setOf(early.id, "gone@1/due"), desired = listOf(early, onTime, other))
        assertEquals(listOf(onTime, other), sync.toSchedule)
        assertEquals(setOf("gone@1/due"), sync.toCancel)
    }

    @Test
    fun syncingTwiceChangesNothing() {
        val desired = listOf(early, onTime)
        val first = AlertSync.diff(emptySet(), desired)
        val scheduled = first.toSchedule.map { it.id }.toSet()
        val second = AlertSync.diff(scheduled, desired)
        assertEquals(emptyList(), second.toSchedule)
        assertEquals(emptySet(), second.toCancel)
    }
}
