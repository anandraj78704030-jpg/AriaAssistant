package com.aria.assistant.core

/**
 * The single source of truth for what Aria is "doing" right now.
 * The Compose UI reacts to this to animate the orb, so keep it as the
 * only place that decides the assistant's visible state.
 */
sealed class AssistantState {
    data object Idle : AssistantState()
    data object Listening : AssistantState()
    data object Thinking : AssistantState()
    data class Executing(val actionLabel: String) : AssistantState()
    data class Success(val message: String, val emotion: Emotion = Emotion.WARM) : AssistantState()
    data class Error(val message: String) : AssistantState()
}
