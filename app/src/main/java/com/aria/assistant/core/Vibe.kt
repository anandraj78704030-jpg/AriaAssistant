package com.aria.assistant.core

/**
 * Picks randomly among a few phrasings of the same reply so Aria
 * doesn't say the exact same sentence every single time — a small
 * thing, but it's a big part of not sounding robotic/repetitive.
 */
object Vibe {
    fun pick(vararg options: String): String = options.random()
}
