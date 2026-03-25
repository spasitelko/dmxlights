package com.dmxlights.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dmxlights.model.DmxCue
import com.dmxlights.model.DmxScene

@Composable
fun CueEditDialog(
    initialCue: DmxCue?,
    defaultTimestampMs: Long,
    onSave: (DmxCue) -> Unit,
    onDismiss: () -> Unit
) {
    val cue = initialCue ?: DmxCue(timestampMs = defaultTimestampMs, scene = DmxScene())

    var label by remember { mutableStateOf(cue.scene.label) }
    var timestampText by remember {
        mutableStateOf(formatTimestamp(cue.timestampMs))
    }
    val channels = remember {
        mutableStateMapOf<Int, Int>().apply { putAll(cue.scene.channels) }
    }
    var showAddChannel by remember { mutableStateOf(false) }
    var newChannelText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialCue != null) "Edit Cue" else "New Cue") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = timestampText,
                    onValueChange = { timestampText = it },
                    label = { Text("Timestamp (MM:SS.mmm)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Scene Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Channels", style = MaterialTheme.typography.labelLarge)
                    OutlinedButton(onClick = { showAddChannel = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Channel")
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    items(channels.keys.sorted()) { ch ->
                        ChannelSliderRow(
                            channel = ch,
                            value = channels[ch] ?: 0,
                            onValueChange = { channels[ch] = it },
                            onRemove = { channels.remove(ch) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val ts = parseTimestamp(timestampText) ?: cue.timestampMs
                val newCue = DmxCue(
                    timestampMs = ts,
                    scene = DmxScene(label = label, channels = channels.toMap()),
                    fadeMs = cue.fadeMs
                )
                onSave(newCue)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showAddChannel) {
        AlertDialog(
            onDismissRequest = { showAddChannel = false; newChannelText = "" },
            title = { Text("Add Channel") },
            text = {
                OutlinedTextField(
                    value = newChannelText,
                    onValueChange = { newChannelText = it },
                    label = { Text("Channel (1-512)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val ch = newChannelText.toIntOrNull()
                    if (ch != null && ch in 1..512) {
                        channels[ch] = 0
                    }
                    showAddChannel = false
                    newChannelText = ""
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddChannel = false; newChannelText = "" }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ChannelSliderRow(
    channel: Int,
    value: Int,
    onValueChange: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Ch $channel",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(48.dp)
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..255f,
            modifier = Modifier.weight(1f)
        )
        Text(
            "$value",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(32.dp)
        )
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Delete, contentDescription = "Remove", modifier = Modifier)
        }
    }
}

private fun formatTimestamp(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millis = ms % 1000
    return "%02d:%02d.%03d".format(minutes, seconds, millis)
}

private fun parseTimestamp(text: String): Long? {
    val regex = Regex("""(\d+):(\d+)\.(\d+)""")
    val match = regex.matchEntire(text.trim()) ?: return null
    val minutes = match.groupValues[1].toLongOrNull() ?: return null
    val seconds = match.groupValues[2].toLongOrNull() ?: return null
    val millis = match.groupValues[3].toLongOrNull() ?: return null
    return minutes * 60_000 + seconds * 1000 + millis
}
