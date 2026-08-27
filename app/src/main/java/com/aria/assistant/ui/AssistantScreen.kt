package com.aria.assistant.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aria.assistant.core.AssistantState
import com.aria.assistant.core.Emotion

private val BgColor = Color(0xFF0A0A12)
private val OrbIdle = Color(0xFF3A3FD9)
private val OrbListening = Color(0xFF33D1C9)
private val OrbThinking = Color(0xFFB06AF7)
private val OrbSuccess = Color(0xFF39E27A)
private val OrbError = Color(0xFFE94F5C)

private fun emotionColor(emotion: Emotion): Color = when (emotion) {
    Emotion.WARM -> Color(0xFFE8A33D)
    Emotion.CONCERNED -> Color(0xFFE9C13D)
    Emotion.EXCITED -> Color(0xFF39E27A)
    Emotion.CALM -> Color(0xFF3AC9D9)
    Emotion.PLAYFUL -> Color(0xFFE85DC0)
    Emotion.PROFESSIONAL -> Color(0xFF6A7FE0)
    Emotion.SURPRISED -> Color(0xFFF77F3D)
}

@Composable
fun AssistantScreen(viewModel: AssistantViewModel = viewModel()) {
    val showMemoryViewer by viewModel.showMemoryViewer.collectAsState()

    if (showMemoryViewer) {
        MemoryViewerScreen(viewModel)
    } else {
        MainAssistantScreen(viewModel)
    }
}

@Composable
private fun MainAssistantScreen(viewModel: AssistantViewModel) {
    val state by viewModel.state.collectAsState()
    val transcript by viewModel.transcript.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(BgColor)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val handsFree by viewModel.handsFreeEnabled.collectAsState()
            Text(
                text = if (handsFree) "🎧 Hands-free: ON" else "🎧 Hands-free: OFF",
                color = if (handsFree) Color(0xFF39E27A) else Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp,
                modifier = Modifier.clickable { viewModel.toggleHandsFree() }
            )
            Text(
                text = "🧠 Memory",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp,
                modifier = Modifier.clickable { viewModel.toggleMemoryViewer() }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AiOrb(state = state, onClick = { viewModel.onMicPressed() })

            Box(modifier = Modifier.size(24.dp))

            Text(
                text = stateLabel(state),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp
            )

            Box(modifier = Modifier.size(12.dp))

            Text(
                text = responseText(state, transcript),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun MemoryViewerScreen(viewModel: AssistantViewModel) {
    val entries by viewModel.memoryEntries.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "← Back",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                modifier = Modifier.clickable { viewModel.toggleMemoryViewer() }
            )
            if (entries.isNotEmpty()) {
                Text(
                    text = "Clear All",
                    color = Color(0xFFE94F5C),
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { viewModel.clearAllMemories() }
                )
            }
        }

        Box(modifier = Modifier.size(16.dp))

        Text(
            text = "What Aria remembers",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Box(modifier = Modifier.size(16.dp))

        if (entries.isEmpty()) {
            Text(
                text = "Nothing remembered yet. Try saying \"yaad rakho...\" to Aria.",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp
            )
        } else {
            LazyColumn {
                items(entries) { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = entry.value,
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "✕",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp,
                            modifier = Modifier.clickable { viewModel.deleteMemoryEntry(entry.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AiOrb(state: AssistantState, onClick: () -> Unit) {
    val baseColor = when (state) {
        is AssistantState.Idle -> OrbIdle
        is AssistantState.Listening -> OrbListening
        is AssistantState.Thinking -> OrbThinking
        is AssistantState.Executing -> OrbThinking
        is AssistantState.Success -> emotionColor(state.emotion)
        is AssistantState.Error -> OrbError
    }

    val infiniteTransition = rememberInfiniteTransition(label = "orb-pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (state is AssistantState.Listening || state is AssistantState.Thinking) 1.12f else 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb-pulse-value"
    )

    Box(
        modifier = Modifier
            .size(160.dp)
            .scale(pulse)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(baseColor, baseColor.copy(alpha = 0.15f))
                ),
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = micGlyph(state),
            fontSize = 40.sp
        )
    }
}

private fun micGlyph(state: AssistantState): String = when (state) {
    is AssistantState.Listening -> "🎙️"
    is AssistantState.Thinking -> "…"
    is AssistantState.Executing -> "⚙️"
    is AssistantState.Success -> "✓"
    is AssistantState.Error -> "!"
    is AssistantState.Idle -> "🎤"
}

private fun stateLabel(state: AssistantState): String = when (state) {
    is AssistantState.Idle -> "Tap to speak"
    is AssistantState.Listening -> "Listening…"
    is AssistantState.Thinking -> "Thinking…"
    is AssistantState.Executing -> "Executing: ${state.actionLabel}"
    is AssistantState.Success -> "Aria"
    is AssistantState.Error -> "Error"
}

private fun responseText(state: AssistantState, transcript: String): String = when (state) {
    is AssistantState.Success -> state.message
    is AssistantState.Error -> state.message
    is AssistantState.Listening -> if (transcript.isNotBlank()) transcript else " "
    else -> " "
}
