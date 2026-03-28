package com.polyinsights.nfccloner.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.polyinsights.nfccloner.data.model.ScannedCard

@Database(entities = [ScannedCard::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
}
