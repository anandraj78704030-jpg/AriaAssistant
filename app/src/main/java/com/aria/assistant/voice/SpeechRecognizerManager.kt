package com.aria.assistant.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Thin wrapper around Android's built-in SpeechRecognizer.
 *
 * Why the built-in one and not a cloud STT SDK for Stage 1: it needs zero
 * extra API keys, works offline on most devices, and is the fastest path
 * to a working MVP. We can swap this out later without touching the ViewModel,
 * since it only exposes callbacks (onResult / onError / onListeningStateChanged).
 */
class SpeechRecognizerManager(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onListeningStateChanged: (Boolean) -> Unit
) {
    private var recognizer: SpeechRecognizer? = null

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition isn't available on this device.")
            return
        }

        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    onListeningStateChanged(true)
                }

                override fun onResults(results: Bundle?) {
                    onListeningStateChanged(false)
                    val text = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                    if (text.isNullOrBlank()) {
                        onError("I didn't catch that.")
                    } else {
                        onResult(text)
                    }
                }

                override fun onError(error: Int) {
                    onListeningStateChanged(false)
                    onError(mapErrorCode(error))
                }

                override fun onEndOfSpeech() {
                    onListeningStateChanged(false)
                }

                // Unused callbacks — required by the interface, intentionally no-ops.
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                // Hinglish-friendly: prefer Indian English locale, but let the
                // recognizer fall back to device default if unsupported.
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            }
            startListening(intent)
        }
    }

    fun stopListening() {
        recognizer?.stopListening()
    }

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
    }

    private fun mapErrorCode(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH -> "I didn't catch that — try again?"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "I didn't hear anything."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "I need microphone permission to listen."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network issue with speech recognition."
        else -> "Something went wrong while listening."
    }
}
