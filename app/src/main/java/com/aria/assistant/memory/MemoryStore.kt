package com.aria.assistant.memory

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Simple local memory store. Everything stays on-device — nothing is
 * ever sent anywhere. add() replaces an existing entry with the same
 * category+label rather than duplicating it, so re-stating a
 * preference updates it instead of piling up copies.
 */
class MemoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("aria_memory", Context.MODE_PRIVATE)
    private val storageKey = "entries"

    fun getAll(): List<MemoryEntry> {
        val raw = prefs.getString(storageKey, null) ?: return emptyList()
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            MemoryEntry(
                id = o.getString("id"),
                category = MemoryCategory.valueOf(o.getString("category")),
                label = o.getString("label"),
                value = o.getString("value"),
                timestamp = o.getLong("timestamp")
            )
        }
    }

    fun add(category: MemoryCategory, label: String, value: String) {
        val entries = getAll().toMutableList()
        entries.removeAll { it.category == category && it.label.equals(label, ignoreCase = true) }
        entries.add(MemoryEntry(UUID.randomUUID().toString(), category, label, value, System.currentTimeMillis()))
        saveAll(entries)
    }

    fun delete(id: String) {
        saveAll(getAll().filterNot { it.id == id })
    }

    fun clearAll() {
        prefs.edit().remove(storageKey).apply()
    }

    fun find(category: MemoryCategory, label: String): MemoryEntry? =
        getAll().firstOrNull { it.category == category && it.label.equals(label, ignoreCase = true) }

    private fun saveAll(entries: List<MemoryEntry>) {
        val arr = JSONArray()
        entries.forEach { e ->
            val o = JSONObject()
            o.put("id", e.id)
            o.put("category", e.category.name)
            o.put("label", e.label)
            o.put("value", e.value)
            o.put("timestamp", e.timestamp)
            arr.put(o)
        }
        prefs.edit().putString(storageKey, arr.toString()).apply()
    }
}
