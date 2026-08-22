package com.aria.assistant.actions

/**
 * Outcome of a real Android action. Deliberately just two cases —
 * Aria never reports Success unless the action actually happened.
 * A missing permission or a failed launch is always Failure with an
 * honest explanation, never a silently "faked" success.
 */
sealed class ActionResult {
    data class Success(val message: String) : ActionResult()
    data class Failure(val message: String) : ActionResult()
}
