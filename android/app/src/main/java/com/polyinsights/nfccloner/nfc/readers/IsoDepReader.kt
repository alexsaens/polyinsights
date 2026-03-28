package com.polyinsights.nfccloner.nfc.readers

import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.NfcA
import com.polyinsights.nfccloner.data.model.ApduExchange
import com.polyinsights.nfccloner.data.model.CardType
import com.polyinsights.nfccloner.data.model.ScannedCard
import com.polyinsights.nfccloner.util.HexUtil
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IsoDepReader @Inject constructor() : TagReader {

    override fun canRead(tag: Tag): Boolean {
        return tag.techList.contains(IsoDep::class.java.name)
    }

    override suspend fun read(tag: Tag): ReadResult {
        val isoDep = IsoDep.get(tag) ?: return ReadResult.Failure("Could not get IsoDep from tag")

        return try {
            isoDep.connect()
            isoDep.timeout = 5000 // 5 second timeout

            val apduLog = mutableListOf<ApduExchange>()
            val errors = mutableListOf<String>()

            // Probe with standard APDU commands to discover the card's applications
            val probeCommands = buildProbeCommands()

            for (command in probeCommands) {
                try {
                    val response = isoDep.transceive(command)
                    if (response.isNotEmpty()) {
                        apduLog.add(
                            ApduExchange(
                                command = HexUtil.bytesToHex(command),
                                response = HexUtil.bytesToHex(response)
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Command not supported — skip silently
                }
            }

            val uid = HexUtil.bytesToHex(tag.id)
            val nfcA = NfcA.get(tag)
            val atqa = nfcA?.atqa?.let { HexUtil.bytesToHex(it) }
            val sak = nfcA?.sak?.let { String.format("%02X", it.toInt()) }

            val historicalBytes = isoDep.historicalBytes?.let { HexUtil.bytesToHex(it) }
            val hiLayerResponse = isoDep.hiLayerResponse?.let { HexUtil.bytesToHex(it) }

            val rawInfo = buildString {
                if (historicalBytes != null) appendLine("Historical Bytes: $historicalBytes")
                if (hiLayerResponse != null) appendLine("HiLayer Response: $hiLayerResponse")
                appendLine("Max Transceive Length: ${isoDep.maxTransceiveLength}")
                appendLine("Timeout: ${isoDep.timeout}ms")
                appendLine("Extended Length Supported: ${isoDep.isExtendedLengthApduSupported}")
            }

            val card = ScannedCard(
                uid = uid,
                cardType = CardType.ISODEP,
                techList = Json.encodeToString(tag.techList.toList()),
                atqa = atqa,
                sak = sak,
                apduLogJson = Json.encodeToString(apduLog),
                rawDataHex = rawInfo
            )

            if (errors.isEmpty()) ReadResult.Success(card)
            else ReadResult.PartialRead(card, errors)

        } catch (e: Exception) {
            ReadResult.Failure("Error reading IsoDep: ${e.message}")
        } finally {
            try { isoDep.close() } catch (_: Exception) {}
        }
    }

    private fun buildProbeCommands(): List<ByteArray> {
        val commands = mutableListOf<ByteArray>()

        // SELECT by DF name — common AIDs
        val commonAids = listOf(
            "325041592E5359532E4444463031",  // 2PAY.SYS.DDF01 (Proximity Payment)
            "315041592E5359532E4444463031",  // 1PAY.SYS.DDF01
            "A000000003101001",               // Visa
            "A0000000041010",                 // Mastercard
            "D2760000850101",                 // NDEF Application
            "F0000000000000",                 // Generic test AID
        )

        for (aid in commonAids) {
            val aidBytes = HexUtil.hexToBytes(aid)
            // SELECT command: CLA=00, INS=A4, P1=04 (by name), P2=00, Lc, AID
            val command = ByteArray(5 + aidBytes.size + 1)
            command[0] = 0x00.toByte() // CLA
            command[1] = 0xA4.toByte() // INS: SELECT
            command[2] = 0x04.toByte() // P1: Select by DF name
            command[3] = 0x00.toByte() // P2
            command[4] = aidBytes.size.toByte() // Lc
            System.arraycopy(aidBytes, 0, command, 5, aidBytes.size)
            command[command.size - 1] = 0x00.toByte() // Le
            commands.add(command)
        }

        // GET DATA commands
        commands.add(byteArrayOf(0x00, 0xCA.toByte(), 0x00, 0x6F, 0x00)) // FCI
        commands.add(byteArrayOf(0x00, 0xCA.toByte(), 0x00, 0x4F, 0x00)) // ADF Name

        return commands
    }
}
