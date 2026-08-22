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
import com.aria.assistant.planner.TaskPlanner
import com.aria.assistant.safety.RiskClassifier
import com.aria.assistant.safety.RiskLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Stage 10: hands-free "Hey Pixie" wake word — built with Android's own
 * SpeechRecognizer in a continuous listen-restart loop, rather than a
 * dedicated wake-word library. Trade-off, stated plainly:
 *  - Uses more battery than a lightweight dedicated wake-word engine
 *  - Needs internet, since Android's built-in recognizer is mostly cloud-based
 *  - A persistent notification is required — Android's own transparency
 *    rule for background mic use, not something Pixie can hide
 * In exchange: zero new dependencies, nothing new that can break the build.
 */
class WakeWordService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var recognizer: SpeechRecognizerManager? = null
    private var tts: TextToSpeechManager? = null
    private val conversationContext = ConversationContext()
    private lateinit var memoryStore: MemoryStore
    private var pendingCommand: String? = null
    private var awaitingCommand = false
    private var running = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        memoryStore = MemoryStore(applicationContext)
        tts = TextToSpeechManager(applicationContext) { }
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!running) {
            running = true
            listenLoop()
        }
        return START_STICKY
    }

    private fun listenLoop() {
        recognizer?.destroy()
        recognizer = SpeechRecognizerManager(
            context = applicationContext,
            onResult = { text -> serviceScope.launch { handleHeard(text) } },
            onError = { serviceScope.launch { restartListening() } },
            onListeningStateChanged = { }
        )
        recognizer?.startListening()
    }

    private suspend fun restartListening() {
        delay(500)
        if (running) recognizer?.startListening()
    }

    private suspend fun handleHeard(text: String) {
        val lower = text.lowercase()

        if (awaitingCommand) {
            awaitingCommand = false
            processCommand(text)
            restartListening()
            return
        }

        val pending = pendingCommand
        if (pending != null) {
            pendingCommand = null
            when {
                containsAny(lower, "haan", "yes", "karo", "confirm", "kar do", "pakka") -> runCommand(pending)
                containsAny(lower, "nahi", "no", "cancel", "mat karo", "ruk jao") -> speak("Theek hai, cancel kar diya.")
            }
            restartListening()
            return
        }

        if (containsAny(lower, "pixie", "aria", "arya")) {
            val afterWake = listOf("pixie", "aria", "arya")
                .map { lower.substringAfter(it, missingDelimiterValue = "") }
                .firstOrNull { it.isNotBlank() }
                ?.trim()

            if (!afterWake.isNullOrBlank()) {
                processCommand(afterWake)
            } else {
                speak("Haan bolo!")
                awaitingCommand = true
            }
        }

        restartListening()
    }

    private suspend fun processCommand(text: String) {
        if (RiskClassifier.classify(text) == RiskLevel.HIGH) {
            pendingCommand = text
            speak("Ye thoda risky hai — pakka karna hai? Haan ya nahi bolo.")
            return
        }
        runCommand(text)
    }

    private suspend fun runCommand(text: String) {
        val steps = TaskPlanner.splitSteps(text)
        val response = TaskPlanner.executeSteps(steps, applicationContext, conversationContext, memoryStore) { }
        speak(response.text)
    }

    private fun speak(text: String) {
        tts?.speak(text, Emotion.WARM)
    }

    private fun containsAny(text: String, vararg phrases: String) = phrases.any { it in text }

    private fun buildNotification(): Notification {
        val channelId = "pixie_wakeword"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Pixie hands-free", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Pixie is listening")
            .setContentText("Say \"Hey Pixie\" anytime")
            .setSmallIcon(android.R.drawable.stat_sys_speakerphone)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        running = false
        recognizer?.destroy()
        tts?.destroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 42
    }
}
