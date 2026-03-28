package com.polyinsights.nfccloner.ui.screens.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polyinsights.nfccloner.data.model.ScannedCard
import com.polyinsights.nfccloner.data.repository.CardRepository
import com.polyinsights.nfccloner.nfc.NfcManager
import com.polyinsights.nfccloner.nfc.readers.IsoDepReader
import com.polyinsights.nfccloner.nfc.readers.MifareClassicReader
import com.polyinsights.nfccloner.nfc.readers.MifareUltralightReader
import com.polyinsights.nfccloner.nfc.readers.NdefTagReader
import com.polyinsights.nfccloner.nfc.readers.ReadResult
import com.polyinsights.nfccloner.nfc.readers.TagReader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val nfcManager: NfcManager,
    private val cardRepository: CardRepository,
    mifareClassicReader: MifareClassicReader,
    mifareUltralightReader: MifareUltralightReader,
    ndefTagReader: NdefTagReader,
    isoDepReader: IsoDepReader
) : ViewModel() {

    // Priority order: specific readers first, generic last
    private val readers: List<TagReader> = listOf(
        mifareClassicReader,
        mifareUltralightReader,
        isoDepReader,
        ndefTagReader
    )

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.WaitingForTag)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            nfcManager.tagFlow.collect { tag ->
                _uiState.value = ScanUiState.Reading(nfcManager.detectCardType(tag).displayName)

                val reader = readers.firstOrNull { it.canRead(tag) }
                if (reader == null) {
                    // Fallback: create a basic card entry from tag info
                    val cardType = nfcManager.detectCardType(tag)
                    val uid = com.polyinsights.nfccloner.util.HexUtil.bytesToHex(tag.id)
                    val card = ScannedCard(
                        uid = uid,
                        cardType = cardType,
                        techList = kotlinx.serialization.json.Json.encodeToString(
                            kotlinx.serialization.builtins.ListSerializer(kotlinx.serialization.builtins.serializer<String>()),
                            tag.techList.toList()
                        )
                    )
                    _uiState.value = ScanUiState.Result(card, listOf("No compatible reader for this card type"))
                    return@collect
                }

                when (val result = reader.read(tag)) {
                    is ReadResult.Success -> {
                        _uiState.value = ScanUiState.Result(result.card, emptyList())
                    }
                    is ReadResult.PartialRead -> {
                        _uiState.value = ScanUiState.Result(result.card, result.errors)
                    }
                    is ReadResult.Failure -> {
                        _uiState.value = ScanUiState.Error(result.reason)
                    }
                }
            }
        }
    }

    fun saveCard(card: ScannedCard, label: String = "") {
        viewModelScope.launch {
            val cardToSave = if (label.isNotBlank()) card.copy(label = label) else card
            val id = cardRepository.saveCard(cardToSave)
            _uiState.value = ScanUiState.Saved(id)
        }
    }

    fun resetToWaiting() {
        _uiState.value = ScanUiState.WaitingForTag
    }
}

sealed class ScanUiState {
    data object WaitingForTag : ScanUiState()
    data class Reading(val cardTypeName: String) : ScanUiState()
    data class Result(val card: ScannedCard, val errors: List<String>) : ScanUiState()
    data class Error(val message: String) : ScanUiState()
    data class Saved(val cardId: Long) : ScanUiState()
}
