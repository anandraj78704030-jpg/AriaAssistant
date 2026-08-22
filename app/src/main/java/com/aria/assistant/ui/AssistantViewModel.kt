package com.aria.assistant.ui

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aria.assistant.core.AssistantState
import com.aria.assistant.core.ConversationContext
import com.aria.assistant.core.Emotion
import com.aria.assistant.memory.MemoryEntry
import com.aria.assistant.memory.MemoryStore
import com.aria.assistant.planner.TaskPlanner
import com.aria.assistant.safety.RiskClassifier
import com.aria.assistant.safety.RiskLevel
import com.aria.assistant.voice.SpeechRecognizerManager
import com.aria.assistant.voice.TextToSpeechManager
import com.aria.assistant.voice.WakeWordService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<AssistantState>(AssistantState.Idle)
    val state: StateFlow<AssistantState> = _state.asStateFlow()

    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript.asStateFlow()

    private val conversationContext = ConversationContext()
    private val memoryStore = MemoryStore(application)

    // Stage 8: a HIGH-risk command waits here for a yes/no reply
    // before it's actually executed.
    private var pendingCommand: String? = null

    private val _showMemoryViewer = MutableStateFlow(false)
    val showMemoryViewer: StateFlow<Boolean> = _showMemoryViewer.asStateFlow()

    private val _handsFreeEnabled = MutableStateFlow(false)
    val handsFreeEnabled: StateFlow<Boolean> = _handsFreeEnabled.asStateFlow()

    private val _memoryEntries = MutableStateFlow<List<MemoryEntry>>(emptyList())
    val memoryEntries: StateFlow<List<MemoryEntry>> = _memoryEntries.asStateFlow()

    private val tts = TextToSpeechManager(application) { speaking ->
        if (!speaking && _state.value !is AssistantState.Listening) {
            _state.value = AssistantState.Idle
        }
    }

    private val speechRecognizer = SpeechRecognizerManager(
        context = application,
        onResult = ::handleRecognizedSpeech,
        onError = { message ->
            _state.value = AssistantState.Error(message)
            tts.speak(message, Emotion.CONCERNED)
        },
        onListeningStateChanged = { isListening ->
            if (isListening) _state.value = AssistantState.Listening
        }
    )

    fun onMicPressed() {
        _transcript.value = ""
        speechRecognizer.startListening()
    }

    fun toggleMemoryViewer() {
        _showMemoryViewer.value = !_showMemoryViewer.value
        if (_showMemoryViewer.value) refreshMemories()
    }

    fun toggleHandsFree() {
        val app = getApplication<Application>()
        val serviceIntent = Intent(app, WakeWordService::class.java)
        if (_handsFreeEnabled.value) {
            app.stopService(serviceIntent)
            _handsFreeEnabled.value = false
        } else {
            ContextCompat.startForegroundService(app, serviceIntent)
            _handsFreeEnabled.value = true
        }
    }

    fun deleteMemoryEntry(id: String) {
        memoryStore.delete(id)
        refreshMemories()
    }

    fun clearAllMemories() {
        memoryStore.clearAll()
        refreshMemories()
    }

    private fun refreshMemories() {
        _memoryEntries.value = memoryStore.getAll()
    }

    private fun handleRecognizedSpeech(text: String) {
        _transcript.value = text
        _state.value = AssistantState.Thinking

        viewModelScope.launch {
            delay(300)

            val pending = pendingCommand
            if (pending != null) {
                pendingCommand = null
                val lower = text.lowercase()
                when {
                    containsAny(lower, "haan", "yes", "karo", "confirm", "kar do", "pakka") -> {
                        runCommand(pending)
                        return@launch
                    }
                    containsAny(lower, "nahi", "no", "cancel", "mat karo", "ruk jao") -> {
                        val msg = "Theek hai, cancel kar diya."
                        _state.value = AssistantState.Success(msg, Emotion.CALM)
                        tts.speak(msg, Emotion.CALM)
                        return@launch
                    }
                    // Anything else: drop the pending confirmation and
                    // treat this as a brand-new command instead.
                }
            }

            if (RiskClassifier.classify(text) == RiskLevel.HIGH) {
                pendingCommand = text
                val msg = "Arey ruko — ye thoda risky action hai. Pakka karna hai? Haan ya nahi bolo."
                _state.value = AssistantState.Success(msg, Emotion.CONCERNED)
                tts.speak(msg, Emotion.CONCERNED)
                return@launch
            }

            runCommand(text)
        }
    }

    private suspend fun runCommand(text: String) {
        val steps = TaskPlanner.splitSteps(text)
        val response = TaskPlanner.executeSteps(
            steps, getApplication(), conversationContext, memoryStore
        ) { step -> _state.value = AssistantState.Executing(step) }

        _state.value = AssistantState.Success(response.text, response.emotion)
        tts.speak(response.text, response.emotion)
    }

    private fun containsAny(text: String, vararg phrases: String): Boolean = phrases.any { it in text }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer.destroy()
        tts.destroy()
    }
}
