package com.dmxlights.artnet

object ArtNetPacket {

    private val HEADER = byteArrayOf(
        'A'.code.toByte(), 'r'.code.toByte(), 't'.code.toByte(), '-'.code.toByte(),
        'N'.code.toByte(), 'e'.code.toByte(), 't'.code.toByte(), 0x00
    )

    private const val OPCODE_LO = 0x00.toByte()  // OpDmx 0x5000 little-endian
    private const val OPCODE_HI = 0x50.toByte()

    private const val PROTO_VER_HI = 0x00.toByte()  // Version 14, big-endian
    private const val PROTO_VER_LO = 0x0E.toByte()

    private const val DMX_LENGTH = 512
    private const val PACKET_SIZE = 18 + DMX_LENGTH  // 530 bytes

    fun build(dmxData: ByteArray, universe: Int, sequence: Int): ByteArray {
        require(dmxData.size == DMX_LENGTH) { "DMX data must be exactly 512 bytes" }
        require(universe in 0..32767) { "Universe must be 0-32767" }
        require(sequence in 0..255) { "Sequence must be 0-255" }

        val packet = ByteArray(PACKET_SIZE)

        // Header: "Art-Net\0"
        HEADER.copyInto(packet, 0)

        // OpCode: 0x5000 little-endian
        packet[8] = OPCODE_LO
        packet[9] = OPCODE_HI

        // Protocol version: 14, big-endian
        packet[10] = PROTO_VER_HI
        packet[11] = PROTO_VER_LO

        // Sequence
        packet[12] = sequence.toByte()

        // Physical port
        packet[13] = 0x00

        // Universe: little-endian
        packet[14] = (universe and 0xFF).toByte()
        packet[15] = ((universe shr 8) and 0x7F).toByte()

        // Length: big-endian
        packet[16] = ((DMX_LENGTH shr 8) and 0xFF).toByte()
        packet[17] = (DMX_LENGTH and 0xFF).toByte()

        // DMX data
        dmxData.copyInto(packet, 18)

        return packet
    }
}
