package com.aria.assistant.core

/**
 * Tracks short-term conversation state so follow-up commands work —
 * e.g. "Open YouTube" then "Search GTA videos" resolves "search" to
 * mean "search on YouTube" without repeating the app name.
 *
 * This is session-only (cleared when the app process dies) and is
 * NOT the long-term memory system — that's a separate, later stage
 * (preferences, routines, contacts persisted across sessions).
 */
class ConversationContext {
    var lastApp: String? = null
        private set

    private val history = ArrayDeque<Pair<String, String>>() // user text to Aria's reply
    private val maxHistory = 5

    fun rememberApp(appName: String) {
        lastApp = appName
    }

    fun recordExchange(userText: String, ariaText: String) {
        history.addLast(userText to ariaText)
        if (history.size > maxHistory) history.removeFirst()
    }

    fun clear() {
        lastApp = null
        history.clear()
    }
}
