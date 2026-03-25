package com.dmxlights.ui.live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.dmxlights.artnet.ArtNetSender
import com.dmxlights.data.ShowRepository
import com.dmxlights.model.Show
import com.dmxlights.playback.AudioPlayer
import com.dmxlights.playback.SyncEngine
import com.dmxlights.ui.settings.getOdeIp
import com.dmxlights.ui.settings.getUniverse
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LivePlaybackScreen(
    showId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { ShowRepository(context) }
    val scope = rememberCoroutineScope()

    val audioPlayer = remember { AudioPlayer(context) }
    val artNetSender = remember { ArtNetSender() }
    val syncEngine = remember { SyncEngine(audioPlayer, artNetSender) }

    var show by remember { mutableStateOf<Show?>(null) }
    val isPlaying by audioPlayer.isPlaying.collectAsState()
    val duration by audioPlayer.duration.collectAsState()
    var currentPosition by remember { mutableLongStateOf(0L) }
    val currentCue by syncEngine.currentCue.collectAsState()
    val dmxOutput by syncEngine.dmxOutput.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            syncEngine.release()
            audioPlayer.release()
        }
    }

    LaunchedEffect(Unit) {
        show = repository.loadShow(showId)
        show?.let { s ->
            val audioFile = repository.getAudioFile(s.id, s.audioFileName)
            if (audioFile.exists()) {
                audioPlayer.load(audioFile.toUri())
            }
            syncEngine.targetIp = getOdeIp(context)
            syncEngine.loadShow(s.copy(universe = getUniverse(context)))
            syncEngine.startSync(scope)
        }
    }

    LaunchedEffect(isPlaying) {
        while (isActive) {
            currentPosition = audioPlayer.currentPositionMs
            delay(16)
        }
    }

    val currentShow = show

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentShow?.name ?: "Live Playback") },
                navigationIcon = {
                    IconButton(onClick = {
                        syncEngine.sendBlackout(scope)
                        audioPlayer.stop()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Large transport controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (isPlaying) audioPlayer.pause() else audioPlayer.play()
                    },
                    modifier = Modifier
                        .height(64.dp)
                        .width(160.dp)
                ) {
                    if (isPlaying) {
                        Text("PAUSE", style = MaterialTheme.typography.titleLarge)
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PLAY", style = MaterialTheme.typography.titleLarge)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                OutlinedButton(
                    onClick = {
                        audioPlayer.stop()
                        currentPosition = 0
                        syncEngine.onSeek(0)
                    },
                    modifier = Modifier.height(64.dp)
                ) {
                    Text("STOP", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(modifier = Modifier.width(16.dp))
                OutlinedButton(
                    onClick = { syncEngine.sendBlackout(scope) },
                    modifier = Modifier.height(64.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("BLACKOUT", style = MaterialTheme.typography.titleLarge)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Position display
            Text(
                formatMs(currentPosition) + " / " + formatMs(duration),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Seek slider
            if (duration > 0) {
                Slider(
                    value = currentPosition.toFloat(),
                    onValueChange = { pos ->
                        val posMs = pos.toLong()
                        audioPlayer.seekTo(posMs)
                        syncEngine.onSeek(posMs)
                        currentPosition = posMs
                    },
                    valueRange = 0f..duration.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Current cue
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Current Cue: ", style = MaterialTheme.typography.labelLarge)
                    Text(
                        currentCue?.scene?.label ?: "(none)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // DMX channel monitor (first 32 channels)
            Text("DMX Output", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(4.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items((0 until 32).toList()) { ch ->
                        val value = dmxOutput.getOrElse(ch) { 0 }.toInt() and 0xFF
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(16.dp)
                                    .fillMaxHeight()
                                    .weight(1f)
                            ) {
                                // Background
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant
                                        )
                                )
                                // Filled portion
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(value / 255f)
                                        .align(Alignment.BottomCenter)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                            Text(
                                "${ch + 1}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Status
            Text(
                "Target: ${syncEngine.targetIp} | Universe: ${syncEngine.universe}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millis = ms % 1000
    return "%02d:%02d.%03d".format(minutes, seconds, millis)
}
