package com.dmxlights.model

import kotlinx.serialization.Serializable

@Serializable
data class DmxScene(
    val label: String = "",
    val channels: Map<Int, Int> = emptyMap()
)
