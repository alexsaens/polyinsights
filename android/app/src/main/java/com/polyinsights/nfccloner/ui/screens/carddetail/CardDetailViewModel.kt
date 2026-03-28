package com.polyinsights.nfccloner.ui.screens.carddetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polyinsights.nfccloner.data.model.ApduExchange
import com.polyinsights.nfccloner.data.model.ScannedCard
import com.polyinsights.nfccloner.data.model.SectorData
import com.polyinsights.nfccloner.data.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class CardDetailViewModel @Inject constructor(
    private val cardRepository: CardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CardDetailUiState>(CardDetailUiState.Loading)
    val uiState: StateFlow<CardDetailUiState> = _uiState.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    fun loadCard(cardId: Long) {
        viewModelScope.launch {
            val card = cardRepository.getCardById(cardId)
            if (card == null) {
                _uiState.value = CardDetailUiState.NotFound
                return@launch
            }

            val sectors = card.sectorDataJson?.let {
                try { json.decodeFromString<List<SectorData>>(it) } catch (_: Exception) { null }
            }

            val apduLog = card.apduLogJson?.let {
                try { json.decodeFromString<List<ApduExchange>>(it) } catch (_: Exception) { null }
            }

            _uiState.value = CardDetailUiState.Loaded(
                card = card,
                sectors = sectors,
                apduLog = apduLog
            )
        }
    }

    fun deleteCard(cardId: Long, onDeleted: () -> Unit) {
        viewModelScope.launch {
            cardRepository.deleteCardById(cardId)
            onDeleted()
        }
    }

    fun updateLabel(card: ScannedCard, newLabel: String) {
        viewModelScope.launch {
            cardRepository.updateCard(card.copy(label = newLabel))
            loadCard(card.id)
        }
    }

    fun updateNotes(card: ScannedCard, newNotes: String) {
        viewModelScope.launch {
            cardRepository.updateCard(card.copy(notes = newNotes))
            loadCard(card.id)
        }
    }
}

sealed class CardDetailUiState {
    data object Loading : CardDetailUiState()
    data object NotFound : CardDetailUiState()
    data class Loaded(
        val card: ScannedCard,
        val sectors: List<SectorData>?,
        val apduLog: List<ApduExchange>?
    ) : CardDetailUiState()
}
