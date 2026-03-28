package com.polyinsights.nfccloner.nfc.readers

import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NfcA
import com.polyinsights.nfccloner.data.model.CardType
import com.polyinsights.nfccloner.data.model.ScannedCard
import com.polyinsights.nfccloner.util.HexUtil
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NdefTagReader @Inject constructor() : TagReader {

    override fun canRead(tag: Tag): Boolean {
        return tag.techList.contains(Ndef::class.java.name)
    }

    override suspend fun read(tag: Tag): ReadResult {
        val ndef = Ndef.get(tag) ?: return ReadResult.Failure("Could not get NDEF from tag")

        return try {
            ndef.connect()
            val ndefMessage = ndef.ndefMessage
            val ndefContent = if (ndefMessage != null) {
                buildString {
                    appendLine("Type: ${ndef.type}")
                    appendLine("Max Size: ${ndef.maxSize} bytes")
                    appendLine("Is Writable: ${ndef.isWritable}")
                    appendLine("Records: ${ndefMessage.records.size}")
                    appendLine()
                    for ((index, record) in ndefMessage.records.withIndex()) {
                        appendLine("--- Record $index ---")
                        appendLine("TNF: ${tnfToString(record.tnf)}")
                        if (record.type.isNotEmpty()) {
                            appendLine("Type: ${String(record.type)}")
                        }
                        if (record.id != null && record.id.isNotEmpty()) {
                            appendLine("ID: ${HexUtil.bytesToHex(record.id)}")
                        }
                        val payload = record.payload
                        if (payload.isNotEmpty()) {
                            // Try to decode as text
                            try {
                                val text = parseNdefTextPayload(payload)
                                if (text != null) {
                                    appendLine("Text: $text")
                                } else {
                                    appendLine("Payload (hex): ${HexUtil.bytesToHex(payload)}")
                                    val str = String(payload).filter { it.isLetterOrDigit() || it.isWhitespace() || it in ":/.-_?&=" }
                                    if (str.length > 3) {
                                        appendLine("Payload (text): $str")
                                    }
                                }
                            } catch (_: Exception) {
                                appendLine("Payload (hex): ${HexUtil.bytesToHex(payload)}")
                            }
                        }
                        appendLine()
                    }
                }
            } else {
                "No NDEF message found (tag may be empty)"
            }

            val uid = HexUtil.bytesToHex(tag.id)
            val nfcA = NfcA.get(tag)
            val atqa = nfcA?.atqa?.let { HexUtil.bytesToHex(it) }
            val sak = nfcA?.sak?.let { String.format("%02X", it.toInt()) }

            val card = ScannedCard(
                uid = uid,
                cardType = CardType.NDEF_GENERIC,
                techList = Json.encodeToString(tag.techList.toList()),
                atqa = atqa,
                sak = sak,
                ndef = ndefContent
            )

            ReadResult.Success(card)

        } catch (e: Exception) {
            ReadResult.Failure("Error reading NDEF: ${e.message}")
        } finally {
            try { ndef.close() } catch (_: Exception) {}
        }
    }

    private fun parseNdefTextPayload(payload: ByteArray): String? {
        if (payload.isEmpty()) return null
        val statusByte = payload[0].toInt()
        val langCodeLength = statusByte and 0x3F
        val isUtf16 = (statusByte and 0x80) != 0
        if (1 + langCodeLength >= payload.size) return null
        val charset = if (isUtf16) Charsets.UTF_16 else Charsets.UTF_8
        return String(payload, 1 + langCodeLength, payload.size - 1 - langCodeLength, charset)
    }

    private fun tnfToString(tnf: Short): String = when (tnf) {
        0.toShort() -> "Empty"
        1.toShort() -> "NFC Forum well-known type"
        2.toShort() -> "Media-type (RFC 2046)"
        3.toShort() -> "Absolute URI (RFC 3986)"
        4.toShort() -> "NFC Forum external type"
        5.toShort() -> "Unknown"
        6.toShort() -> "Unchanged"
        else -> "Reserved ($tnf)"
    }
}
