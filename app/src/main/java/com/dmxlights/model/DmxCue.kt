package com.dmxlights.model

import kotlinx.serialization.Serializable

@Serializable
data class DmxCue(
    val timestampMs: Long,
    val scene: DmxScene,
    val fadeMs: Long = 0
)
