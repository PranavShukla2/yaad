package com.pranavshukla.yaad.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.db.SqlDriver
import com.pranavshukla.yaad.core.model.Category
import com.pranavshukla.yaad.data.db.YaadDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.coroutines.CoroutineContext
import com.pranavshukla.yaad.data.db.Category as CategoryRow

/** The user's own categories and their settings (plain key-value text; Settings in Phase 10 gives them types). */
class PreferenceStore(driver: SqlDriver, private val context: CoroutineContext = Dispatchers.Default) {
    private val q = YaadDatabase(driver).yaadQueries

    fun categories(): Flow<List<Category>> =
        q.selectCategories().asFlow().mapToList(context).map { rows -> rows.map { Category(it.id, it.name, it.color) } }

    fun saveCategory(category: Category) = q.transaction {
        q.deleteCategory(category.id)
        q.insertCategory(CategoryRow(category.id, category.name, category.color))
    }

    fun deleteCategory(id: String) = q.deleteCategory(id)

    fun setting(key: String): String? = q.selectSetting(key).executeAsOneOrNull()

    fun putSetting(key: String, value: String) = q.putSetting(key, value)
}
