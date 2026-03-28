package com.polyinsights.nfccloner.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NfcA
import android.os.Build
import com.polyinsights.nfccloner.data.model.CardType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NfcManager @Inject constructor(
    private val context: Context
) {
    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)

    private val _tagFlow = MutableSharedFlow<Tag>(replay = 0, extraBufferCapacity = 1)
    val tagFlow: SharedFlow<Tag> = _tagFlow.asSharedFlow()

    private val _nfcState = MutableStateFlow(getNfcState())
    val nfcState: StateFlow<NfcState> = _nfcState.asStateFlow()

    enum class NfcState {
        NOT_AVAILABLE,
        DISABLED,
        ENABLED
    }

    fun refreshNfcState() {
        _nfcState.value = getNfcState()
    }

    private fun getNfcState(): NfcState {
        return when {
            nfcAdapter == null -> NfcState.NOT_AVAILABLE
            !nfcAdapter.isEnabled -> NfcState.DISABLED
            else -> NfcState.ENABLED
        }
    }

    fun enableForegroundDispatch(activity: Activity) {
        val adapter = nfcAdapter ?: return
        val intent = Intent(activity, activity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(activity, 0, intent, flags)
        val filters = arrayOf(
            IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
        )
        val techLists = arrayOf(
            arrayOf(IsoDep::class.java.name),
            arrayOf(MifareClassic::class.java.name),
            arrayOf(MifareUltralight::class.java.name),
            arrayOf(Ndef::class.java.name),
            arrayOf(NfcA::class.java.name)
        )
        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techLists)
        refreshNfcState()
    }

    fun disableForegroundDispatch(activity: Activity) {
        val adapter = nfcAdapter ?: return
        adapter.disableForegroundDispatch(activity)
    }

    suspend fun onTagDiscovered(tag: Tag) {
        _tagFlow.emit(tag)
    }

    fun detectCardType(tag: Tag): CardType {
        val techList = tag.techList.toList()
        return when {
            techList.contains(MifareClassic::class.java.name) -> {
                val mc = MifareClassic.get(tag)
                when (mc?.type) {
                    MifareClassic.TYPE_CLASSIC -> {
                        if (mc.size == 1024) CardType.MIFARE_CLASSIC_1K
                        else CardType.MIFARE_CLASSIC_4K
                    }
                    else -> CardType.MIFARE_CLASSIC_1K
                }
            }
            techList.contains(MifareUltralight::class.java.name) -> {
                val mu = MifareUltralight.get(tag)
                when (mu?.type) {
                    MifareUltralight.TYPE_ULTRALIGHT -> CardType.MIFARE_ULTRALIGHT
                    MifareUltralight.TYPE_ULTRALIGHT_C -> CardType.MIFARE_ULTRALIGHT_C
                    else -> CardType.MIFARE_ULTRALIGHT
                }
            }
            techList.contains(IsoDep::class.java.name) -> CardType.ISODEP
            techList.contains(Ndef::class.java.name) -> CardType.NDEF_GENERIC
            techList.contains("android.nfc.tech.NfcB") -> CardType.NFC_B
            techList.contains("android.nfc.tech.NfcF") -> CardType.NFC_F
            techList.contains("android.nfc.tech.NfcV") -> CardType.NFC_V
            techList.contains(NfcA::class.java.name) -> CardType.NFC_A
            else -> CardType.UNKNOWN
        }
    }
}
