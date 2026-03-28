package com.polyinsights.nfccloner.data.repository

import com.polyinsights.nfccloner.data.db.CardDao
import com.polyinsights.nfccloner.data.model.ScannedCard
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardRepository @Inject constructor(
    private val cardDao: CardDao
) {
    fun getAllCards(): Flow<List<ScannedCard>> = cardDao.getAllCards()

    fun getRecentCards(limit: Int = 5): Flow<List<ScannedCard>> = cardDao.getRecentCards(limit)

    suspend fun getCardById(id: Long): ScannedCard? = cardDao.getCardById(id)

    suspend fun saveCard(card: ScannedCard): Long = cardDao.insertCard(card)

    suspend fun updateCard(card: ScannedCard) = cardDao.updateCard(card)

    suspend fun deleteCard(card: ScannedCard) = cardDao.deleteCard(card)

    suspend fun deleteCardById(id: Long) = cardDao.deleteCardById(id)
}
