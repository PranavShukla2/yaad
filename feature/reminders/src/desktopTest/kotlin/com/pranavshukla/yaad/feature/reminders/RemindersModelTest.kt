package com.pranavshukla.yaad.feature.reminders

import com.pranavshukla.yaad.core.model.ChecklistItem
import com.pranavshukla.yaad.core.model.OccurrenceStatus
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.model.Schedule
import com.pranavshukla.yaad.core.scheduling.AlertAction
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RemindersModelTest {
    private val zone = TimeZone.of("Asia/Kolkata")
    private var changes = 0

    private fun TestScope.model(): RemindersModel {
        val dispatcher = StandardTestDispatcher(testScheduler)
        return RemindersModel(testStore(dispatcher), backgroundScope, TestClock, { zone }, { changes++ }, dispatcher, emptyFlow())
    }

    private fun call(model: RemindersModel) =
        Reminder(model.newId(), "Call mom", Schedule(LocalDateTime(2026, 10, 28, 19, 0)), checklist = listOf(ChecklistItem("Ask about Diwali")))

    @Test
    fun savingShowsTheReminderOnTodayAndReplans() = runTest {
        val model = model()
        model.save(call(model))
        runCurrent()
        assertEquals(listOf("Call mom"), model.state.value.agenda.today.map { it.reminder.title })
        assertEquals(1, changes)
    }

    @Test
    fun doneCanBeUndone() = runTest {
        val model = model()
        model.save(call(model))
        runCurrent()
        val item = model.state.value.agenda.today.single()
        model.act(item, AlertAction.Done)
        runCurrent()
        assertTrue(model.state.value.agenda.today.single().isHandled)
        model.undo(item)
        runCurrent()
        assertEquals(OccurrenceStatus.Pending, model.state.value.agenda.today.single().status)
        assertEquals(3, changes)
    }

    @Test
    fun checklistItemsAreTicked() = runTest {
        val model = model()
        val reminder = call(model)
        model.save(reminder)
        model.toggleChecklist(reminder, 0)
        runCurrent()
        assertEquals(listOf(true), model.state.value.reminders.single().checklist.map { it.done })
    }

    @Test
    fun deletingRemovesItEverywhere() = runTest {
        val model = model()
        val reminder = call(model)
        model.save(reminder)
        runCurrent()
        model.delete(reminder.id)
        runCurrent()
        assertEquals(emptyList(), model.state.value.reminders)
        assertEquals(emptyList(), model.state.value.agenda.today)
    }
}
