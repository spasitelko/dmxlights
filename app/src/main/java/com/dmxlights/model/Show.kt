package com.dmxlights.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Show(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val audioFileName: String,
    val cues: List<DmxCue> = emptyList(),
    val universe: Int = 0
)
