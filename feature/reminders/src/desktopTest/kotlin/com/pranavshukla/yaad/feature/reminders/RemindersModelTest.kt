package com.pranavshukla.yaad.feature.reminders

import com.pranavshukla.yaad.core.model.ChecklistItem
import com.pranavshukla.yaad.core.model.OccurrenceStatus
import com.pranavshukla.yaad.core.model.Reminder
import com.pranavshukla.yaad.core.model.Schedule
import com.pranavshukla.yaad.core.scheduling.AlertAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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

    @Test
    fun manySavesAtOnceAllLand() = runBlocking {
        // Found by the screenshot test: overlapping saves on several threads broke SQLite transactions.
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val model = RemindersModel(testStore(Dispatchers.Default), scope, TestClock, { zone }, {}, Dispatchers.Default, emptyFlow())
        repeat(20) { model.save(Reminder(model.newId(), "R$it", Schedule(LocalDateTime(2026, 10, 29, 9, 0)))) }
        withTimeout(5_000) { model.state.first { it.reminders.size == 20 } }
        scope.cancel()
    }

    @Test
    fun undoAfterTravelClearsTheRecordMadeInTheOtherZone() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        var zoneNow = TimeZone.of("Asia/Kolkata")
        val store = testStore(dispatcher)
        val model = RemindersModel(store, backgroundScope, TestClock, { zoneNow }, {}, dispatcher, emptyFlow())
        val daily = Reminder(model.newId(), "Pill", Schedule(LocalDateTime(2026, 10, 28, 21, 0), com.pranavshukla.yaad.core.model.RecurrenceRule(com.pranavshukla.yaad.core.model.Frequency.Daily())))
        model.save(daily)
        runCurrent()
        model.act(model.state.value.agenda.today.single(), AlertAction.Done)
        runCurrent()
        // Fly to London: the reminder list is read again in the new zone; today's 21:00 is still done.
        zoneNow = TimeZone.of("Europe/London")
        model.save(daily) // any change recomputes the state
        runCurrent()
        val item = model.state.value.agenda.today.single { it.reminder.id == daily.id }
        assertTrue(item.isHandled)
        model.undo(item)
        runCurrent()
        assertEquals(emptyList(), store.occurrences(daily.id), "the Kolkata record is cleared, not left behind")
    }
}
