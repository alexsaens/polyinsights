package com.polyinsights.nfccloner.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.polyinsights.nfccloner.data.model.ScannedCard
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {

    @Query("SELECT * FROM scanned_cards ORDER BY scannedAt DESC")
    fun getAllCards(): Flow<List<ScannedCard>>

    @Query("SELECT * FROM scanned_cards WHERE id = :id")
    suspend fun getCardById(id: Long): ScannedCard?

    @Query("SELECT * FROM scanned_cards ORDER BY scannedAt DESC LIMIT :limit")
    fun getRecentCards(limit: Int = 5): Flow<List<ScannedCard>>

    @Query("SELECT * FROM scanned_cards WHERE cardType = :type ORDER BY scannedAt DESC")
    fun getCardsByType(type: String): Flow<List<ScannedCard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: ScannedCard): Long

    @Update
    suspend fun updateCard(card: ScannedCard)

    @Delete
    suspend fun deleteCard(card: ScannedCard)

    @Query("DELETE FROM scanned_cards WHERE id = :id")
    suspend fun deleteCardById(id: Long)
}
