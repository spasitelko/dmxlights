package com.dmxlights.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.dmxlights.data.ShowRepository
import com.dmxlights.model.DmxCue
import com.dmxlights.model.Show
import com.dmxlights.playback.AudioPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowEditorScreen(
    showId: String,
    onBack: () -> Unit,
    onLaunchLive: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { ShowRepository(context) }
    val scope = rememberCoroutineScope()

    var show by remember { mutableStateOf<Show?>(null) }
    var editingCueIndex by remember { mutableStateOf<Int?>(null) }
    var showNewCueDialog by remember { mutableStateOf(false) }

    val audioPlayer = remember { AudioPlayer(context) }
    val isPlaying by audioPlayer.isPlaying.collectAsState()
    val duration by audioPlayer.duration.collectAsState()
    var currentPosition by remember { mutableLongStateOf(0L) }

    DisposableEffect(Unit) {
        onDispose { audioPlayer.release() }
    }

    LaunchedEffect(Unit) {
        show = repository.loadShow(showId)
        show?.let { s ->
            val audioFile = repository.getAudioFile(s.id, s.audioFileName)
            if (audioFile.exists()) {
                audioPlayer.load(audioFile.toUri())
            }
        }
    }

    LaunchedEffect(isPlaying) {
        while (isActive && isPlaying) {
            currentPosition = audioPlayer.currentPositionMs
            delay(50)
        }
    }

    fun saveShow(updatedShow: Show) {
        show = updatedShow
        scope.launch { repository.saveShow(updatedShow) }
    }

    val currentShow = show ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentShow.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onLaunchLive) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("LIVE")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewCueDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Cue")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Playback controls
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = {
                            if (isPlaying) audioPlayer.pause() else audioPlayer.play()
                        }) {
                            Text(if (isPlaying) "||" else ">>",
                                style = MaterialTheme.typography.titleMedium)
                        }
                        IconButton(onClick = { audioPlayer.stop(); currentPosition = 0 }) {
                            Text("[]", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            formatMs(currentPosition),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(" / ", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            formatMs(duration),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (duration > 0) {
                        LinearProgressIndicator(
                            progress = { (currentPosition.toFloat() / duration).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "Cues (${currentShow.cues.size})",
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (currentShow.cues.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No cues yet. Tap + to add one.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(currentShow.cues.sortedBy { it.timestampMs }) { index, cue ->
                        CueRow(
                            cue = cue,
                            onClick = { editingCueIndex = index },
                            onDelete = {
                                val updatedCues = currentShow.cues.sortedBy { it.timestampMs }
                                    .toMutableList().apply { removeAt(index) }
                                saveShow(currentShow.copy(cues = updatedCues))
                            }
                        )
                    }
                }
            }
        }
    }

    if (showNewCueDialog) {
        CueEditDialog(
            initialCue = null,
            defaultTimestampMs = currentPosition,
            onSave = { newCue ->
                val updatedCues = currentShow.cues + newCue
                saveShow(currentShow.copy(cues = updatedCues))
                showNewCueDialog = false
            },
            onDismiss = { showNewCueDialog = false }
        )
    }

    editingCueIndex?.let { index ->
        val sortedCues = currentShow.cues.sortedBy { it.timestampMs }
        if (index in sortedCues.indices) {
            CueEditDialog(
                initialCue = sortedCues[index],
                defaultTimestampMs = currentPosition,
                onSave = { editedCue ->
                    val updatedCues = sortedCues.toMutableList().apply { set(index, editedCue) }
                    saveShow(currentShow.copy(cues = updatedCues))
                    editingCueIndex = null
                },
                onDismiss = { editingCueIndex = null }
            )
        }
    }
}

@Composable
private fun CueRow(
    cue: DmxCue,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                formatMs(cue.timestampMs),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.width(80.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    cue.scene.label.ifEmpty { "(unnamed)" },
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "${cue.scene.channels.size} ch",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error)
            }
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
