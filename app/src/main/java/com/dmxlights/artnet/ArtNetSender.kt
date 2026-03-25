package com.dmxlights.artnet

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class ArtNetSender {

    private var socket: DatagramSocket? = null
    private var sequence = 1

    private fun nextSequence(): Int {
        val seq = sequence
        sequence = if (sequence >= 255) 1 else sequence + 1
        return seq
    }

    suspend fun send(
        dmxData: ByteArray,
        universe: Int,
        targetIp: String,
        port: Int = 6454
    ) = withContext(Dispatchers.IO) {
        if (socket == null || socket?.isClosed == true) {
            socket = DatagramSocket()
        }
        val packet = ArtNetPacket.build(dmxData, universe, nextSequence())
        val address = InetAddress.getByName(targetIp)
        val datagram = DatagramPacket(packet, packet.size, address, port)
        socket!!.send(datagram)
    }

    fun close() {
        socket?.close()
        socket = null
        sequence = 1
    }
}
