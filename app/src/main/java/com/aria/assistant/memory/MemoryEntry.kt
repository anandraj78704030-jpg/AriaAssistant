package com.aria.assistant.memory

data class MemoryEntry(
    val id: String,
    val category: MemoryCategory,
    val label: String,
    val value: String,
    val timestamp: Long
)
