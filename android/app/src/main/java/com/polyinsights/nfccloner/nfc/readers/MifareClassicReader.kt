package com.polyinsights.nfccloner.nfc.readers

import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.MifareClassic
import android.nfc.tech.NfcA
import com.polyinsights.nfccloner.data.model.CardType
import com.polyinsights.nfccloner.data.model.ScannedCard
import com.polyinsights.nfccloner.data.model.SectorData
import com.polyinsights.nfccloner.nfc.keys.KeyManager
import com.polyinsights.nfccloner.util.HexUtil
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MifareClassicReader @Inject constructor(
    private val keyManager: KeyManager
) : TagReader {

    override fun canRead(tag: Tag): Boolean {
        return tag.techList.contains(MifareClassic::class.java.name)
    }

    override suspend fun read(tag: Tag): ReadResult {
        val mfc = MifareClassic.get(tag) ?: return ReadResult.Failure("Could not get MifareClassic from tag")

        return try {
            mfc.connect()
            val sectors = mutableListOf<SectorData>()
            val errors = mutableListOf<String>()
            val keys = keyManager.getAllKeys()

            for (sectorIndex in 0 until mfc.sectorCount) {
                val sectorResult = readSector(mfc, sectorIndex, keys)
                when (sectorResult) {
                    is SectorReadResult.Success -> sectors.add(sectorResult.data)
                    is SectorReadResult.AuthFailed -> {
                        sectors.add(
                            SectorData(
                                sectorIndex = sectorIndex,
                                blocks = emptyList(),
                                isReadable = false
                            )
                        )
                        errors.add("Sector $sectorIndex: authentication failed")
                    }
                    is SectorReadResult.Error -> {
                        sectors.add(
                            SectorData(
                                sectorIndex = sectorIndex,
                                blocks = emptyList(),
                                isReadable = false
                            )
                        )
                        errors.add("Sector $sectorIndex: ${sectorResult.message}")
                    }
                }
            }

            val uid = HexUtil.bytesToHex(tag.id)
            val nfcA = NfcA.get(tag)
            val atqa = nfcA?.atqa?.let { HexUtil.bytesToHex(it) }
            val sak = nfcA?.sak?.let { String.format("%02X", it.toInt()) }
            val cardType = if (mfc.size == 1024) CardType.MIFARE_CLASSIC_1K else CardType.MIFARE_CLASSIC_4K

            val card = ScannedCard(
                uid = uid,
                cardType = cardType,
                techList = Json.encodeToString(tag.techList.toList()),
                atqa = atqa,
                sak = sak,
                sectorDataJson = Json.encodeToString(sectors)
            )

            if (errors.isEmpty()) ReadResult.Success(card)
            else ReadResult.PartialRead(card, errors)

        } catch (e: TagLostException) {
            ReadResult.Failure("Tag was removed during read. Please hold the card steady.")
        } catch (e: Exception) {
            ReadResult.Failure("Error reading MIFARE Classic: ${e.message}")
        } finally {
            try { mfc.close() } catch (_: Exception) {}
        }
    }

    private fun readSector(
        mfc: MifareClassic,
        sectorIndex: Int,
        keys: List<ByteArray>
    ): SectorReadResult {
        var authenticatedKeyA: ByteArray? = null
        var authenticatedKeyB: ByteArray? = null

        // Try Key A
        for (key in keys) {
            try {
                if (mfc.authenticateSectorWithKeyA(sectorIndex, key)) {
                    authenticatedKeyA = key
                    break
                }
            } catch (_: Exception) {}
        }

        // Try Key B
        for (key in keys) {
            try {
                if (mfc.authenticateSectorWithKeyB(sectorIndex, key)) {
                    authenticatedKeyB = key
                    break
                }
            } catch (_: Exception) {}
        }

        if (authenticatedKeyA == null && authenticatedKeyB == null) {
            return SectorReadResult.AuthFailed
        }

        return try {
            val firstBlock = mfc.sectorToBlock(sectorIndex)
            val blockCount = mfc.getBlockCountInSector(sectorIndex)
            val blocks = mutableListOf<String>()

            for (blockIndex in 0 until blockCount) {
                try {
                    val blockData = mfc.readBlock(firstBlock + blockIndex)
                    blocks.add(HexUtil.bytesToHex(blockData))
                } catch (e: Exception) {
                    blocks.add("") // empty = unreadable block
                }
            }

            SectorReadResult.Success(
                SectorData(
                    sectorIndex = sectorIndex,
                    blocks = blocks,
                    keyA = authenticatedKeyA?.let { HexUtil.bytesToHex(it) },
                    keyB = authenticatedKeyB?.let { HexUtil.bytesToHex(it) },
                    isReadable = true
                )
            )
        } catch (e: TagLostException) {
            SectorReadResult.Error("Tag lost")
        } catch (e: Exception) {
            SectorReadResult.Error(e.message ?: "Unknown error")
        }
    }

    private sealed class SectorReadResult {
        data class Success(val data: SectorData) : SectorReadResult()
        data object AuthFailed : SectorReadResult()
        data class Error(val message: String) : SectorReadResult()
    }
}
