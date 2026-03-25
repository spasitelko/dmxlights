package com.dmxlights.ui.showlist

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dmxlights.data.ShowRepository
import com.dmxlights.model.Show
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowListScreen(
    onShowClick: (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { ShowRepository(context) }
    val scope = rememberCoroutineScope()

    var shows by remember { mutableStateOf<List<Show>>(emptyList()) }
    var showNewDialog by remember { mutableStateOf(false) }
    var newShowName by remember { mutableStateOf("") }
    var pendingAudioUri by remember { mutableStateOf<Uri?>(null) }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingAudioUri = uri
            showNewDialog = true
        }
    }

    fun refreshShows() {
        scope.launch { shows = repository.listShows() }
    }

    LaunchedEffect(Unit) { refreshShows() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DMX Lights") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Show")
            }
        }
    ) { padding ->
        if (shows.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No shows yet. Tap + to create one.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(shows, key = { it.id }) { show ->
                    ShowCard(
                        show = show,
                        onClick = { onShowClick(show.id) },
                        onDelete = {
                            scope.launch {
                                repository.deleteShow(show.id)
                                refreshShows()
                            }
                        }
                    )
                }
            }
        }

        if (showNewDialog) {
            AlertDialog(
                onDismissRequest = {
                    showNewDialog = false
                    newShowName = ""
                    pendingAudioUri = null
                },
                title = { Text("New Show") },
                text = {
                    OutlinedTextField(
                        value = newShowName,
                        onValueChange = { newShowName = it },
                        label = { Text("Show Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val uri = pendingAudioUri ?: return@TextButton
                            val name = newShowName.ifBlank { "Untitled Show" }
                            scope.launch {
                                val show = Show(name = name, audioFileName = "")
                                val audioFileName = repository.importAudio(show.id, uri)
                                val updatedShow = show.copy(audioFileName = audioFileName)
                                repository.saveShow(updatedShow)
                                refreshShows()
                                showNewDialog = false
                                newShowName = ""
                                pendingAudioUri = null
                                onShowClick(updatedShow.id)
                            }
                        },
                        enabled = newShowName.isNotBlank()
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showNewDialog = false
                        newShowName = ""
                        pendingAudioUri = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun ShowCard(
    show: Show,
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(show.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${show.audioFileName} - ${show.cues.size} cue(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
