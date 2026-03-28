package com.polyinsights.nfccloner.nfc.hce

import android.content.Intent
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import com.polyinsights.nfccloner.data.model.ApduExchange
import com.polyinsights.nfccloner.util.HexUtil
import kotlinx.serialization.json.Json

/**
 * HCE service that replays APDU responses captured during card scanning.
 * When a reader sends a command APDU, this service looks up the matching
 * response from the saved card data and sends it back.
 *
 * Limitations:
 * - Only works for IsoDep (ISO 14443-4) based cards
 * - UID is randomized by Android — cannot replay the original card's UID
 * - Cannot emulate MIFARE Classic, Ultralight, or other low-level protocols
 */
class CardEmulationService : HostApduService() {

    companion object {
        private const val TAG = "CardEmulationService"
        const val EXTRA_APDU_LOG_JSON = "apdu_log_json"
        const val EXTRA_CARD_LABEL = "card_label"
        const val ACTION_START_EMULATION = "com.polyinsights.nfccloner.START_EMULATION"
        const val ACTION_STOP_EMULATION = "com.polyinsights.nfccloner.STOP_EMULATION"

        // Standard responses
        private val SW_OK = HexUtil.hexToBytes("9000")
        private val SW_FILE_NOT_FOUND = HexUtil.hexToBytes("6A82")
        private val SW_INS_NOT_SUPPORTED = HexUtil.hexToBytes("6D00")

        @Volatile
        var isEmulating = false
            private set

        @Volatile
        var currentCardLabel: String = ""
            private set
    }

    private var apduResponseMap = mutableMapOf<String, ByteArray>()
    private val json = Json { ignoreUnknownKeys = true }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_EMULATION -> {
                val apduLogJson = intent.getStringExtra(EXTRA_APDU_LOG_JSON)
                currentCardLabel = intent.getStringExtra(EXTRA_CARD_LABEL) ?: "Unknown"

                if (apduLogJson != null) {
                    loadApduResponses(apduLogJson)
                    isEmulating = true
                    Log.i(TAG, "Emulation started for: $currentCardLabel with ${apduResponseMap.size} response mappings")
                }
            }
            ACTION_STOP_EMULATION -> {
                stopEmulation()
            }
        }
        return START_NOT_STICKY
    }

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        val commandHex = HexUtil.bytesToHex(commandApdu)
        Log.d(TAG, "Received APDU: $commandHex")

        // Exact match first
        apduResponseMap[commandHex]?.let {
            Log.d(TAG, "Exact match found, responding: ${HexUtil.bytesToHex(it)}")
            return it
        }

        // Try matching by INS byte (command[1]) + data
        // This handles cases where CLA byte differs
        if (commandApdu.size >= 2) {
            val insBasedKey = apduResponseMap.keys.find { key ->
                val keyBytes = HexUtil.hexToBytes(key)
                keyBytes.size >= 2 && keyBytes[1] == commandApdu[1] &&
                    // Match P1-P2 if present
                    (keyBytes.size < 4 || commandApdu.size < 4 ||
                        (keyBytes[2] == commandApdu[2] && keyBytes[3] == commandApdu[3]))
            }
            insBasedKey?.let {
                val response = apduResponseMap[it]!!
                Log.d(TAG, "INS-based match found, responding: ${HexUtil.bytesToHex(response)}")
                return response
            }
        }

        // Handle SELECT by AID — if we have any SELECT response, return it
        if (commandApdu.size >= 5 && commandApdu[0] == 0x00.toByte() && commandApdu[1] == 0xA4.toByte()) {
            val selectResponse = apduResponseMap.keys
                .filter { HexUtil.hexToBytes(it).let { b -> b.size >= 2 && b[1] == 0xA4.toByte() } }
                .firstNotNullOfOrNull { apduResponseMap[it] }

            if (selectResponse != null) {
                Log.d(TAG, "SELECT fallback match, responding: ${HexUtil.bytesToHex(selectResponse)}")
                return selectResponse
            }
        }

        Log.d(TAG, "No match found, returning 6A82 (file not found)")
        return SW_FILE_NOT_FOUND
    }

    override fun onDeactivated(reason: Int) {
        val reasonStr = when (reason) {
            DEACTIVATION_LINK_LOSS -> "Link loss"
            DEACTIVATION_DESELECTED -> "Deselected"
            else -> "Unknown ($reason)"
        }
        Log.i(TAG, "Deactivated: $reasonStr")
    }

    override fun onDestroy() {
        stopEmulation()
        super.onDestroy()
    }

    private fun loadApduResponses(apduLogJson: String) {
        apduResponseMap.clear()
        try {
            val exchanges = json.decodeFromString<List<ApduExchange>>(apduLogJson)
            for (exchange in exchanges) {
                val responseBytes = HexUtil.hexToBytes(exchange.response)
                // Only store successful responses (SW1 = 90 or 61)
                if (responseBytes.size >= 2) {
                    val sw1 = responseBytes[responseBytes.size - 2].toInt() and 0xFF
                    if (sw1 == 0x90 || sw1 == 0x61) {
                        apduResponseMap[exchange.command] = responseBytes
                    }
                }
            }
            Log.i(TAG, "Loaded ${apduResponseMap.size} APDU response mappings")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse APDU log: ${e.message}")
        }
    }

    private fun stopEmulation() {
        apduResponseMap.clear()
        isEmulating = false
        currentCardLabel = ""
        Log.i(TAG, "Emulation stopped")
    }
}
