package com.aria.assistant.planner

import android.content.Context
import com.aria.assistant.actions.ActionRouter
import com.aria.assistant.core.AriaResponse
import com.aria.assistant.core.ConversationContext
import com.aria.assistant.core.Emotion
import com.aria.assistant.core.PersonalityEngine
import com.aria.assistant.memory.MemoryStore
import kotlinx.coroutines.delay

/**
 * Stage 7: "WhatsApp kholo aur volume badhao" becomes two steps,
 * executed in order through the exact same pipeline a single command
 * would use — nothing about ActionRouter or PersonalityEngine changes.
 * A single-step command (the common case) just becomes a list of one,
 * so this replaces the old direct call without changing that behavior.
 */
object TaskPlanner {

    private val splitPattern = Regex("(?i)\\s+(aur|phir|uske baad|and then|and|then)\\s+")

    fun splitSteps(text: String): List<String> {
        val parts = text.split(splitPattern).map { it.trim() }.filter { it.isNotBlank() }
        return if (parts.size > 1) parts else listOf(text.trim())
    }

    suspend fun executeSteps(
        steps: List<String>,
        context: Context,
        conversationContext: ConversationContext,
        memoryStore: MemoryStore,
        onExecuting: (String) -> Unit
    ): AriaResponse {
        val messages = mutableListOf<String>()
        var lastEmotion = Emotion.CALM

        for (step in steps) {
            val actionResponse = ActionRouter.tryExecute(step, context, conversationContext, memoryStore)
            val response = if (actionResponse != null) {
                onExecuting(step)
                delay(200)
                actionResponse
            } else {
                PersonalityEngine.respond(step, conversationContext)
            }
            conversationContext.recordExchange(step, response.text)
            messages.add(response.text)
            lastEmotion = response.emotion
        }

        return AriaResponse(messages.joinToString(" "), lastEmotion)
    }
}
