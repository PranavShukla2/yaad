package com.pranavshukla.yaad.core.scheduling

import com.pranavshukla.yaad.core.model.Occurrence
import com.pranavshukla.yaad.core.model.OccurrenceStatus
import com.pranavshukla.yaad.core.model.ReminderId
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class AlertActionTest {
    private val zone = TimeZone.of("Asia/Kolkata")
    private val id = ReminderId("rent")
    private val due = Instant.parse("2026-10-28T03:30:00Z")
    private val now = Instant.parse("2026-10-28T16:40:00Z") // 22:10 IST

    private fun apply(action: AlertAction) = action.toOccurrence(id, due, now, zone, morning = LocalTime(9, 0))

    @Test
    fun doneAndSkipRecordTheTime() {
        // The local due time is kept too: 03:30 UTC was 9:00 in Kolkata.
        val local = kotlinx.datetime.LocalDateTime(2026, 10, 28, 9, 0)
        assertEquals(Occurrence(id, due, OccurrenceStatus.Done(now), dueLocal = local), apply(AlertAction.Done))
        assertEquals(Occurrence(id, due, OccurrenceStatus.Skipped(now), dueLocal = local), apply(AlertAction.Skip))
    }

    @Test
    fun snoozesCountFromNow() {
        assertEquals(OccurrenceStatus.Snoozed(now + 10.minutes), apply(AlertAction.Snooze10Minutes).status)
        assertEquals(OccurrenceStatus.Snoozed(now + 1.hours), apply(AlertAction.Snooze1Hour).status)
    }

    @Test
    fun snoozeUntilTomorrowMeansTomorrowMorning() {
        // 22:10 IST on 28 October → 9:00 IST on 29 October.
        assertEquals(
            OccurrenceStatus.Snoozed(Instant.parse("2026-10-29T03:30:00Z")),
            apply(AlertAction.SnoozeTomorrow).status,
        )
    }

    @Test
    fun actionsHaveStableNames() {
        assertEquals(AlertAction.entries, AlertAction.entries.map { AlertAction.valueOf(it.name) })
    }
}
