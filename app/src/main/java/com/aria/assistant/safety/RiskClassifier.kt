package com.aria.assistant.safety

/**
 * Classifies risk BEFORE a command reaches ActionRouter, so a HIGH-risk
 * command can be intercepted and confirmed first — rather than trying
 * to undo something after it already happened.
 *
 * HIGH: tapping something found on-screen via Accessibility (it could
 * be a "Buy Now" or payment button on any app — Pixie can't always
 * tell), and clearing all remembered data (irreversible).
 * MEDIUM: dialing a number, forgetting one specific memory.
 * LOW: everything else (opening apps, alarms, flashlight, volume,
 * brightness, saving a memory, conversation).
 */
object RiskClassifier {

    fun classify(text: String): RiskLevel {
        val lower = text.lowercase()
        return when {
            containsAny(lower, "option", "select karo", "select kar do", "choose karo", "select kar") -> RiskLevel.HIGH
            containsAny(lower, "sab bhula do", "memory clear karo", "sab memory delete karo", "puri memory clear karo") -> RiskLevel.HIGH
            containsAny(lower, "call", "dial", "phone karo", "call karo") -> RiskLevel.MEDIUM
            "bhula do" in lower -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
    }

    private fun containsAny(text: String, vararg phrases: String): Boolean = phrases.any { it in text }
}
