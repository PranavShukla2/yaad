package com.pranavshukla.yaad.data

import com.pranavshukla.yaad.core.model.Category
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PreferenceStoreTest {
    @Test
    fun categoriesAreSavedRenamedAndDeleted() = runTest {
        val store = PreferenceStore(testDriver(), StandardTestDispatcher(testScheduler))
        store.saveCategory(Category("gym", "Gym", 0xFF0F6A73))
        store.saveCategory(Category("car", "Car"))
        store.saveCategory(Category("gym", "Fitness", 0xFF0F6A73))
        assertEquals(listOf(Category("car", "Car"), Category("gym", "Fitness", 0xFF0F6A73)), store.categories().first())
        store.deleteCategory("car")
        assertEquals(listOf("gym"), store.categories().first().map { it.id })
    }

    @Test
    fun settingsAreTextByKey() {
        val store = PreferenceStore(testDriver())
        assertNull(store.setting("currency"))
        store.putSetting("currency", "INR")
        store.putSetting("currency", "USD")
        assertEquals("USD", store.setting("currency"))
    }
}
