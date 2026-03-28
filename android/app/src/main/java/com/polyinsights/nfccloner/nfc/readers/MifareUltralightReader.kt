package com.polyinsights.nfccloner.nfc.readers

import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.MifareUltralight
import android.nfc.tech.NfcA
import com.polyinsights.nfccloner.data.model.CardType
import com.polyinsights.nfccloner.data.model.ScannedCard
import com.polyinsights.nfccloner.util.HexUtil
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MifareUltralightReader @Inject constructor() : TagReader {

    override fun canRead(tag: Tag): Boolean {
        return tag.techList.contains(MifareUltralight::class.java.name)
    }

    override suspend fun read(tag: Tag): ReadResult {
        val mul = MifareUltralight.get(tag)
            ?: return ReadResult.Failure("Could not get MifareUltralight from tag")

        return try {
            mul.connect()

            val cardType = when (mul.type) {
                MifareUltralight.TYPE_ULTRALIGHT_C -> CardType.MIFARE_ULTRALIGHT_C
                else -> CardType.MIFARE_ULTRALIGHT
            }

            // Read all pages. readPages() reads 4 pages (16 bytes) at a time starting from the given page.
            // Ultralight has 16 pages (64 bytes), Ultralight C has 48 pages (192 bytes).
            val maxPages = when (cardType) {
                CardType.MIFARE_ULTRALIGHT_C -> 44  // pages 0-43 readable
                else -> 16  // pages 0-15
            }

            val allData = ByteArray(maxPages * 4)
            var bytesRead = 0
            var page = 0
            val errors = mutableListOf<String>()

            while (page < maxPages) {
                try {
                    val data = mul.readPages(page) // reads 4 pages = 16 bytes
                    val copyLen = minOf(data.size, (maxPages - page) * 4)
                    System.arraycopy(data, 0, allData, page * 4, copyLen)
                    bytesRead += copyLen
                    page += 4
                } catch (e: TagLostException) {
                    errors.add("Tag lost at page $page")
                    break
                } catch (e: Exception) {
                    errors.add("Error reading page $page: ${e.message}")
                    page += 4
                }
            }

            val uid = HexUtil.bytesToHex(tag.id)
            val nfcA = NfcA.get(tag)
            val atqa = nfcA?.atqa?.let { HexUtil.bytesToHex(it) }
            val sak = nfcA?.sak?.let { String.format("%02X", it.toInt()) }

            val card = ScannedCard(
                uid = uid,
                cardType = cardType,
                techList = Json.encodeToString(tag.techList.toList()),
                atqa = atqa,
                sak = sak,
                pageDataHex = HexUtil.bytesToHex(allData.copyOf(bytesRead))
            )

            if (errors.isEmpty()) ReadResult.Success(card)
            else ReadResult.PartialRead(card, errors)

        } catch (e: TagLostException) {
            ReadResult.Failure("Tag was removed during read. Please hold the card steady.")
        } catch (e: Exception) {
            ReadResult.Failure("Error reading MIFARE Ultralight: ${e.message}")
        } finally {
            try { mul.close() } catch (_: Exception) {}
        }
    }
}
