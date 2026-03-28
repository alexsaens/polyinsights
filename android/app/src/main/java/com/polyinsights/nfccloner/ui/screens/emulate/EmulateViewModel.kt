package com.polyinsights.nfccloner.ui.screens.emulate

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polyinsights.nfccloner.data.model.CardType
import com.polyinsights.nfccloner.data.model.ScannedCard
import com.polyinsights.nfccloner.data.repository.CardRepository
import com.polyinsights.nfccloner.nfc.hce.CardEmulationService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmulateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cardRepository: CardRepository
) : ViewModel() {

    val emulatableCards: StateFlow<List<ScannedCard>> = cardRepository.getAllCards()
        .map { cards -> cards.filter { it.cardType == CardType.ISODEP && it.apduLogJson != null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isEmulating = MutableStateFlow(CardEmulationService.isEmulating)
    val isEmulating: StateFlow<Boolean> = _isEmulating.asStateFlow()

    private val _selectedCard = MutableStateFlow<ScannedCard?>(null)
    val selectedCard: StateFlow<ScannedCard?> = _selectedCard.asStateFlow()

    fun loadPreselectedCard(cardId: Long?) {
        if (cardId == null) return
        viewModelScope.launch {
            _selectedCard.value = cardRepository.getCardById(cardId)
        }
    }

    fun selectCard(card: ScannedCard) {
        _selectedCard.value = card
    }

    fun startEmulation() {
        val card = _selectedCard.value ?: return
        val apduLogJson = card.apduLogJson ?: return

        val intent = Intent(context, CardEmulationService::class.java).apply {
            action = CardEmulationService.ACTION_START_EMULATION
            putExtra(CardEmulationService.EXTRA_APDU_LOG_JSON, apduLogJson)
            putExtra(CardEmulationService.EXTRA_CARD_LABEL, card.label.ifBlank { card.uid })
        }
        context.startService(intent)
        _isEmulating.value = true
    }

    fun stopEmulation() {
        val intent = Intent(context, CardEmulationService::class.java).apply {
            action = CardEmulationService.ACTION_STOP_EMULATION
        }
        context.startService(intent)
        _isEmulating.value = false
    }
}
