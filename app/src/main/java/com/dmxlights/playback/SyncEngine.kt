package com.dmxlights.playback

import com.dmxlights.artnet.ArtNetSender
import com.dmxlights.model.DmxCue
import com.dmxlights.model.Show
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SyncEngine(
    private val audioPlayer: AudioPlayer,
    private val artNetSender: ArtNetSender
) {
    private var show: Show? = null
    private var cues: List<DmxCue> = emptyList()
    private var lastFiredCueIndex: Int = -1
    private val dmxFrame = ByteArray(512)

    private var syncJob: Job? = null

    private val _currentCue = MutableStateFlow<DmxCue?>(null)
    val currentCue: StateFlow<DmxCue?> = _currentCue.asStateFlow()

    private val _dmxOutput = MutableStateFlow(ByteArray(512))
    val dmxOutput: StateFlow<ByteArray> = _dmxOutput.asStateFlow()

    var targetIp: String = "2.0.0.1"
    var universe: Int = 0

    fun loadShow(show: Show) {
        this.show = show
        this.cues = show.cues.sortedBy { it.timestampMs }
        this.universe = show.universe
        resetState()
    }

    fun startSync(scope: CoroutineScope) {
        stopSync()
        syncJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                if (audioPlayer.isPlaying.value) {
                    val posMs = audioPlayer.currentPositionMs
                    advanceCues(posMs)
                    withContext(Dispatchers.IO) {
                        runCatching {
                            artNetSender.send(dmxFrame.copyOf(), universe, targetIp)
                        }
                    }
                }
                delay(10)
            }
        }
    }

    fun stopSync() {
        syncJob?.cancel()
        syncJob = null
    }

    fun onSeek(positionMs: Long) {
        rebuildStateAtPosition(positionMs)
        _dmxOutput.value = dmxFrame.copyOf()
    }

    fun sendBlackout(scope: CoroutineScope) {
        dmxFrame.fill(0)
        _dmxOutput.value = dmxFrame.copyOf()
        _currentCue.value = null
        scope.launch(Dispatchers.IO) {
            runCatching {
                artNetSender.send(dmxFrame.copyOf(), universe, targetIp)
            }
        }
    }

    private fun advanceCues(posMs: Long) {
        while (lastFiredCueIndex + 1 < cues.size &&
            posMs >= cues[lastFiredCueIndex + 1].timestampMs
        ) {
            lastFiredCueIndex++
            applyCue(cues[lastFiredCueIndex])
        }
    }

    private fun applyCue(cue: DmxCue) {
        for ((channel, value) in cue.scene.channels) {
            if (channel in 1..512) {
                dmxFrame[channel - 1] = value.coerceIn(0, 255).toByte()
            }
        }
        _currentCue.value = cue
        _dmxOutput.value = dmxFrame.copyOf()
    }

    private fun rebuildStateAtPosition(posMs: Long) {
        dmxFrame.fill(0)
        lastFiredCueIndex = -1
        for (i in cues.indices) {
            if (cues[i].timestampMs <= posMs) {
                applyCue(cues[i])
                lastFiredCueIndex = i
            } else {
                break
            }
        }
    }

    private fun resetState() {
        dmxFrame.fill(0)
        lastFiredCueIndex = -1
        _currentCue.value = null
        _dmxOutput.value = dmxFrame.copyOf()
    }

    fun release() {
        stopSync()
        artNetSender.close()
    }
}
