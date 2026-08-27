package com.aria.assistant.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.aria.assistant.core.ConversationContext
import com.aria.assistant.core.Emotion
import com.aria.assistant.memory.MemoryStore
import com.aria.assistant.overlay.PixieOverlay
import com.aria.assistant.planner.TaskPlanner
import com.aria.assistant.safety.RiskClassifier
import com.aria.assistant.safety.RiskLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener as VoskRecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService

/**
 * Stage 10 (v3): hands-free wake word using Vosk — a fully offline,
 * open-source speech engine (no account, no company email required,
 * unlike Picovoice which turned out to be enterprise-only). Vosk uses
 * its own AudioRecord-based mic capture rather than Android's
 * assistant-style SpeechRecognizer, so media in other apps is NOT
 * interrupted while Pixie idly listens.
 *
 * Once the wake word is heard, the full Android SpeechRecognizer takes
 * over briefly to capture the actual command (same as before) — that
 * momentary focus-take is expected, same as Alexa/Google Assistant.
 *
 * REQUIRES a Vosk model bundled at app/src/main/assets/model-en-us/
 * (see chat instructions for exact download/placement steps).
 */
class WakeWordService : Service() {

    private enum class Phase { WAKE, COMMAND, CONFIRM }

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var voskModel: Model? = null
    private var wakeRecognizer: Recognizer? = null
    private var wakeSpeechService: SpeechService? = null
    private var commandRecognizer: SpeechRecognizerManager? = null
    private var tts: TextToSpeechManager? = null
    private var overlay: PixieOverlay? = null
    private val conversationContext = ConversationContext()
    private lateinit var memoryStore: MemoryStore
    private var pendingCommand: String? = null
    private var phase = Phase.WAKE

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        memoryStore = MemoryStore(applicationContext)
        tts = TextToSpeechManager(applicationContext) { }
        overlay = PixieOverlay(applicationContext) { err -> updateNotification("⚠️ Overlay: $err") }
        startForeground(NOTIFICATION_ID, buildNotification("⏳ Loading wake-word model..."))
        loadModelThenListen()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun loadModelThenListen() {
        StorageService.unpack(
            applicationContext, "model-en-us", "model",
            { model ->
                voskModel = model
                startWakeListening()
            },
            { exception ->
                updateNotification("⚠️ Model failed to load: ${exception.message}")
            }
        )
    }

    private fun startWakeListening() {
        val model = voskModel ?: return
        try {
            val rec = Recognizer(model, 16000.0f)
            wakeRecognizer = rec
            val service = SpeechService(rec, 16000.0f)
            wakeSpeechService = service
            service.startListening(object : VoskRecognitionListener {
                override fun onResult(hypothesis: String) {
                    val text = JSONObject(hypothesis).optString("text")
                    if (text.isNotBlank()) serviceScope.launch { handleWakePhaseText(text) }
                }
                override fun onFinalResult(hypothesis: String) {}
                override fun onPartialResult(hypothesis: String) {}
                override fun onError(e: Exception) {
                    updateNotification("⚠️ Listening error: ${e.message}")
                }
                override fun onTimeout() {}
            })
            phase = Phase.WAKE
            updateNotification("👂 Listening for \"Pixie\"...")
        } catch (e: Exception) {
            updateNotification("⚠️ Wake listening failed: ${e.javaClass.simpleName} — ${e.message}")
        }
    }

    private fun stopWakeListening() {
        try {
            wakeSpeechService?.stop()
            wakeSpeechService?.shutdown()
        } catch (e: Exception) {
            // Already stopped — safe to ignore.
        }
        wakeSpeechService = null
        wakeRecognizer = null
    }

    private suspend fun handleWakePhaseText(text: String) {
        if (phase != Phase.WAKE) return
        val lower = text.lowercase()
        if (!containsAny(lower, "pixie", "aria", "arya", "pixi")) return

        stopWakeListening()
        phase = Phase.COMMAND

        val afterWake = listOf("pixie", "aria", "arya", "pixi")
            .map { lower.substringAfter(it, missingDelimiterValue = "") }
            .firstOrNull { it.isNotBlank() }
            ?.trim()

        if (!afterWake.isNullOrBlank()) {
            processCommand(afterWake)
        } else {
            speak("Haan bolo!")
            overlay?.show("👂 Haan bolo...", 4000)
            updateNotification("👂 Wake word heard — waiting for your command...")
            startCommandCapture()
        }
    }

    private fun startCommandCapture() {
        commandRecognizer?.destroy()
        commandRecognizer = SpeechRecognizerManager(
            context = applicationContext,
            onResult = { text -> serviceScope.launch { handleCommandResult(text) } },
            onError = { message ->
                serviceScope.launch {
                    updateNotification("⚠️ $message")
                    resumeWakeListening()
                }
            },
            onListeningStateChanged = { }
        )
        commandRecognizer?.startListening()
    }

    private suspend fun handleCommandResult(text: String) {
        if (phase == Phase.CONFIRM) {
            val lower = text.lowercase()
            val pending = pendingCommand
            pendingCommand = null
            if (pending != null && containsAny(lower, "haan", "yes", "karo", "confirm", "kar do", "pakka")) {
                runCommand(pending)
            } else {
                val msg = "Theek hai, cancel kar diya."
                speak(msg)
                overlay?.show(msg, 3000)
                updateNotification(msg)
                resumeWakeListening()
            }
            return
        }
        processCommand(text)
    }

    private suspend fun processCommand(text: String) {
        if (RiskClassifier.classify(text) == RiskLevel.HIGH) {
            pendingCommand = text
            phase = Phase.CONFIRM
            val msg = "Ye thoda risky hai — pakka karna hai? Haan ya nahi bolo."
            speak(msg)
            overlay?.show(msg, 6000)
            updateNotification(msg)
            startCommandCapture()
            return
        }
        runCommand(text)
    }

    private suspend fun runCommand(text: String) {
        overlay?.show("💭 \"$text\"", 0)
        updateNotification("💭 Working on: \"$text\"")
        val steps = TaskPlanner.splitSteps(text)
        val response = TaskPlanner.executeSteps(steps, applicationContext, conversationContext, memoryStore) { }
        speak(response.text)
        overlay?.show(response.text, 5000)
        updateNotification(response.text)
        resumeWakeListening()
    }

    private fun resumeWakeListening() {
        commandRecognizer?.destroy()
        commandRecognizer = null
        startWakeListening()
    }

    private fun speak(text: String) {
        tts?.speak(text, Emotion.WARM)
    }

    private fun containsAny(text: String, vararg phrases: String) = phrases.any { it in text }

    private fun updateNotification(status: String) {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.notify(NOTIFICATION_ID, buildNotification(status))
    }

    private fun buildNotification(status: String = "Say \"Pixie\" anytime"): Notification {
        val channelId = "pixie_wakeword"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Pixie hands-free", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Pixie is listening")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.stat_sys_speakerphone)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopWakeListening()
        commandRecognizer?.destroy()
        tts?.destroy()
        overlay?.hide()
    }

    companion object {
        private const val NOTIFICATION_ID = 42
    }
}
