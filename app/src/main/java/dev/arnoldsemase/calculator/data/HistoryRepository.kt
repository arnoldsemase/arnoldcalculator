package dev.arnoldsemase.calculator.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Domain model. [expression] and [result] are stored in plain form (see NumberFormatter). */
data class HistoryEntry(
    val id: Long = 0,
    val expression: String,
    val result: String,
    val createdAt: Long,
)

class HistoryRepository(private val dao: HistoryDao) {

    val entries: Flow<List<HistoryEntry>> =
        dao.observeAll().map { list -> list.map { HistoryEntry(it.id, it.expression, it.result, it.createdAt) } }

    suspend fun add(expression: String, result: String) {
        dao.insert(HistoryEntity(expression = expression, result = result, createdAt = System.currentTimeMillis()))
        dao.trim(MAX_ENTRIES)
    }

    suspend fun restore(entry: HistoryEntry) {
        dao.insert(HistoryEntity(entry.id, entry.expression, entry.result, entry.createdAt))
    }

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun clear() = dao.clear()

    private companion object {
        const val MAX_ENTRIES = 300
    }
}
