package com.polyinsights.nfccloner.ui.screens.savedcards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polyinsights.nfccloner.data.model.ScannedCard
import com.polyinsights.nfccloner.data.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedCardsViewModel @Inject constructor(
    private val cardRepository: CardRepository
) : ViewModel() {

    val cards: StateFlow<List<ScannedCard>> = cardRepository.getAllCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteCard(card: ScannedCard) {
        viewModelScope.launch {
            cardRepository.deleteCard(card)
        }
    }
}
