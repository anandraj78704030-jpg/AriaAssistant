package com.aria.assistant.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.aria.assistant.core.Emotion
import java.util.Locale
import java.util.UUID

/**
 * Wraps Android's built-in TextToSpeech engine.
 * onSpeakingStateChanged lets the UI/orb animate while Aria is talking.
 */
class TextToSpeechManager(
    context: Context,
    private val onSpeakingStateChanged: (Boolean) -> Unit
) {
    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("en", "IN") // Hinglish-friendly default
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = onSpeakingStateChanged(true)
                    override fun onDone(utteranceId: String?) = onSpeakingStateChanged(false)
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) = onSpeakingStateChanged(false)
                })
                isReady = true
            }
        }
    }

    fun speak(text: String, emotion: Emotion = Emotion.WARM) {
        if (!isReady) return
        val (pitch, rate) = pitchAndRateFor(emotion)
        tts?.setPitch(pitch)
        tts?.setSpeechRate(rate)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }

    /**
     * Small, deliberately subtle adjustments — enough to feel different,
     * not so much it sounds like a cartoon. Android TTS pitch/rate range
     * roughly 0.5–2.0; 1.0 is the engine's natural voice.
     */
    private fun pitchAndRateFor(emotion: Emotion): Pair<Float, Float> = when (emotion) {
        Emotion.EXCITED -> 1.15f to 1.1f
        Emotion.CONCERNED -> 0.95f to 0.9f
        Emotion.CALM -> 0.95f to 0.95f
        Emotion.PLAYFUL -> 1.1f to 1.05f
        Emotion.SURPRISED -> 1.2f to 1.05f
        Emotion.PROFESSIONAL -> 1.0f to 1.0f
        Emotion.WARM -> 1.0f to 1.0f
    }

    fun stop() {
        tts?.stop()
    }

    fun destroy() {
        tts?.shutdown()
        tts = null
    }
}
