package com.polyinsights.nfccloner.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polyinsights.nfccloner.data.model.ScannedCard
import com.polyinsights.nfccloner.data.repository.CardRepository
import com.polyinsights.nfccloner.nfc.NfcManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val nfcManager: NfcManager,
    cardRepository: CardRepository
) : ViewModel() {

    val nfcState: StateFlow<NfcManager.NfcState> = nfcManager.nfcState

    val recentCards: StateFlow<List<ScannedCard>> = cardRepository.getRecentCards(5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun refreshNfcState() = nfcManager.refreshNfcState()

    fun hasAcceptedDisclaimer(): Boolean {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("disclaimer_accepted", false)
    }

    fun acceptDisclaimer() {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("disclaimer_accepted", true).apply()
    }
}
