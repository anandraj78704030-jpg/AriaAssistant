package com.aria.assistant.core

/**
 * The emotional tone behind a response. Drives both the orb's color
 * and the TTS pitch/rate, so Aria "sounds" different when she's
 * concerned vs excited vs calm — not just different words.
 */
enum class Emotion {
    WARM,
    CONCERNED,
    EXCITED,
    CALM,
    PLAYFUL,
    PROFESSIONAL,
    SURPRISED
}
