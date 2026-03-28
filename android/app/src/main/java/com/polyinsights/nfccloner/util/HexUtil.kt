package com.polyinsights.nfccloner.util

object HexUtil {
    private val HEX_CHARS = "0123456789ABCDEF".toCharArray()

    fun bytesToHex(bytes: ByteArray): String {
        val result = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val i = b.toInt()
            result.append(HEX_CHARS[(i shr 4) and 0x0F])
            result.append(HEX_CHARS[i and 0x0F])
        }
        return result.toString()
    }

    fun hexToBytes(hex: String): ByteArray {
        val cleanHex = hex.replace(" ", "").replace(":", "")
        require(cleanHex.length % 2 == 0) { "Hex string must have even length" }
        return ByteArray(cleanHex.length / 2) { i ->
            val index = i * 2
            ((Character.digit(cleanHex[index], 16) shl 4) +
                Character.digit(cleanHex[index + 1], 16)).toByte()
        }
    }

    fun formatHexDump(bytes: ByteArray, bytesPerLine: Int = 16): String {
        val sb = StringBuilder()
        for (i in bytes.indices step bytesPerLine) {
            sb.append(String.format("%04X: ", i))
            val end = minOf(i + bytesPerLine, bytes.size)
            for (j in i until end) {
                sb.append(String.format("%02X ", bytes[j]))
            }
            // Pad if short line
            for (j in end until i + bytesPerLine) {
                sb.append("   ")
            }
            sb.append(" |")
            for (j in i until end) {
                val c = bytes[j].toInt().toChar()
                sb.append(if (c in ' '..'~') c else '.')
            }
            sb.append("|\n")
        }
        return sb.toString()
    }
}
