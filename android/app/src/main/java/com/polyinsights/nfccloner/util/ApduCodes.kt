package com.polyinsights.nfccloner.util

/**
 * ISO 7816-4 APDU status word (SW1-SW2) decoder.
 * Maps response codes to human-readable descriptions.
 */
object ApduCodes {

    fun describe(sw: String): String {
        val clean = sw.uppercase().takeLast(4)
        if (clean.length < 4) return "Unknown (incomplete status word)"

        val sw1 = clean.substring(0, 2)
        val sw2 = clean.substring(2, 4)

        // Exact matches first
        EXACT_CODES[clean]?.let { return it }

        // SW1-based ranges
        return when (sw1) {
            "61" -> "Success — ${"0x$sw2".toInt(16)} bytes of response available (use GET RESPONSE)"
            "62" -> "Warning — State of non-volatile memory unchanged: ${WARNING_62[sw2] ?: "Unknown warning"}"
            "63" -> "Warning — State of non-volatile memory changed: ${WARNING_63[sw2] ?: if (sw2[0] == 'C') "Verification failed, ${sw2[1].digitToInt(16)} retries remaining" else "Unknown warning"}"
            "64" -> "Execution error — State of non-volatile memory unchanged: ${ERROR_64[sw2] ?: "Unknown error"}"
            "65" -> "Execution error — State of non-volatile memory changed: ${ERROR_65[sw2] ?: "Unknown error"}"
            "67" -> "Wrong length (Lc or Le incorrect)"
            "68" -> "Functions in CLA not supported: ${ERROR_68[sw2] ?: "Unknown"}"
            "69" -> "Command not allowed: ${ERROR_69[sw2] ?: "Unknown"}"
            "6A" -> "Wrong parameters P1-P2: ${ERROR_6A[sw2] ?: "Unknown"}"
            "6B" -> "Wrong parameters P1-P2 (out of range)"
            "6C" -> "Wrong Le field — correct Le = 0x$sw2 (${sw2.toInt(16)} bytes)"
            "6D" -> "Instruction code not supported or invalid"
            "6E" -> "Class not supported"
            "6F" -> "No precise diagnosis"
            else -> "Unknown status word: $clean"
        }
    }

    private val EXACT_CODES = mapOf(
        "9000" to "Success",
        "9100" to "Success (MIFARE)",
        "6200" to "Warning: No information given",
        "6281" to "Warning: Part of returned data may be corrupted",
        "6282" to "Warning: End of file/record reached before reading Le bytes",
        "6283" to "Warning: Selected file deactivated",
        "6300" to "Warning: No information given (NV memory changed)",
        "6381" to "Warning: File filled up by last write",
        "6400" to "Execution error: No information given (NV memory unchanged)",
        "6500" to "Execution error: No information given (NV memory changed)",
        "6581" to "Execution error: Memory failure",
        "6700" to "Wrong length",
        "6800" to "Functions in CLA not supported",
        "6881" to "Logical channel not supported",
        "6882" to "Secure messaging not supported",
        "6900" to "Command not allowed",
        "6981" to "Command incompatible with file structure",
        "6982" to "Security status not satisfied",
        "6983" to "Authentication method blocked",
        "6984" to "Referenced data invalidated",
        "6985" to "Conditions of use not satisfied",
        "6986" to "Command not allowed (no current EF)",
        "6987" to "Expected SM data objects missing",
        "6988" to "SM data objects incorrect",
        "6A00" to "Wrong parameters P1-P2",
        "6A80" to "Incorrect parameters in command data field",
        "6A81" to "Function not supported",
        "6A82" to "File or application not found",
        "6A83" to "Record not found",
        "6A84" to "Not enough memory space in the file",
        "6A85" to "Lc inconsistent with TLV structure",
        "6A86" to "Incorrect parameters P1-P2",
        "6A87" to "Lc inconsistent with P1-P2",
        "6A88" to "Referenced data not found",
        "6A89" to "File already exists",
        "6A8A" to "DF name already exists",
        "6B00" to "Wrong parameter(s) P1-P2",
        "6D00" to "Instruction code not supported",
        "6E00" to "Class not supported",
        "6F00" to "No precise diagnosis",
    )

    private val WARNING_62 = mapOf(
        "00" to "No information",
        "81" to "Part of returned data may be corrupted",
        "82" to "End of file reached before reading Le bytes",
        "83" to "Selected file deactivated"
    )

    private val WARNING_63 = mapOf(
        "00" to "No information",
        "81" to "File filled up by last write"
    )

    private val ERROR_64 = mapOf("00" to "No information")
    private val ERROR_65 = mapOf("00" to "No information", "81" to "Memory failure")

    private val ERROR_68 = mapOf(
        "00" to "No information",
        "81" to "Logical channel not supported",
        "82" to "Secure messaging not supported"
    )

    private val ERROR_69 = mapOf(
        "00" to "No information",
        "81" to "Command incompatible with file structure",
        "82" to "Security status not satisfied",
        "83" to "Authentication method blocked",
        "84" to "Referenced data invalidated",
        "85" to "Conditions of use not satisfied",
        "86" to "Command not allowed (no current EF)",
        "87" to "Expected SM data objects missing",
        "88" to "SM data objects incorrect"
    )

    private val ERROR_6A = mapOf(
        "00" to "No information",
        "80" to "Incorrect parameters in data field",
        "81" to "Function not supported",
        "82" to "File or application not found",
        "83" to "Record not found",
        "84" to "Not enough memory",
        "85" to "Lc inconsistent with TLV structure",
        "86" to "Incorrect parameters P1-P2",
        "87" to "Lc inconsistent with P1-P2",
        "88" to "Referenced data not found"
    )
}
