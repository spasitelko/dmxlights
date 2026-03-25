package com.dmxlights.data

import android.content.Context
import android.net.Uri
import com.dmxlights.model.Show
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

class ShowRepository(private val context: Context) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val showsDir: File
        get() = File(context.getExternalFilesDir(null), "shows").also { it.mkdirs() }

    private fun showDir(showId: String): File =
        File(showsDir, showId).also { it.mkdirs() }

    private fun showFile(showId: String): File =
        File(showDir(showId), "show.json")

    suspend fun listShows(): List<Show> = withContext(Dispatchers.IO) {
        showsDir.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { dir ->
                val file = File(dir, "show.json")
                if (file.exists()) {
                    runCatching { json.decodeFromString<Show>(file.readText()) }.getOrNull()
                } else null
            }
            ?.sortedBy { it.name }
            ?: emptyList()
    }

    suspend fun loadShow(showId: String): Show? = withContext(Dispatchers.IO) {
        val file = showFile(showId)
        if (file.exists()) {
            runCatching { json.decodeFromString<Show>(file.readText()) }.getOrNull()
        } else null
    }

    suspend fun saveShow(show: Show) = withContext(Dispatchers.IO) {
        val file = showFile(show.id)
        file.writeText(json.encodeToString(Show.serializer(), show))
    }

    suspend fun deleteShow(showId: String) = withContext(Dispatchers.IO) {
        showDir(showId).deleteRecursively()
    }

    suspend fun importAudio(showId: String, sourceUri: Uri): String = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(sourceUri)
            ?: throw IllegalArgumentException("Cannot open URI: $sourceUri")
        val fileName = resolveFileName(sourceUri)
        val destFile = File(showDir(showId), fileName)
        inputStream.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        fileName
    }

    fun getAudioFile(showId: String, audioFileName: String): File =
        File(showDir(showId), audioFileName)

    private fun resolveFileName(uri: Uri): String {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    return it.getString(nameIndex)
                }
            }
        }
        return uri.lastPathSegment ?: "audio.mp3"
    }
}
