package com.polyinsights.nfccloner.nfc.readers

import android.nfc.Tag
import com.polyinsights.nfccloner.data.model.ScannedCard

interface TagReader {
    fun canRead(tag: Tag): Boolean
    suspend fun read(tag: Tag): ReadResult
}

sealed class ReadResult {
    data class Success(val card: ScannedCard) : ReadResult()
    data class PartialRead(val card: ScannedCard, val errors: List<String>) : ReadResult()
    data class Failure(val reason: String) : ReadResult()
}
