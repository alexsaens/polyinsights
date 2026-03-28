package com.polyinsights.nfccloner.di

import android.content.Context
import androidx.room.Room
import com.polyinsights.nfccloner.data.db.AppDatabase
import com.polyinsights.nfccloner.data.db.CardDao
import com.polyinsights.nfccloner.nfc.NfcManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "nfc_cloner.db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideCardDao(database: AppDatabase): CardDao {
        return database.cardDao()
    }

    @Provides
    @Singleton
    fun provideNfcManager(@ApplicationContext context: Context): NfcManager {
        return NfcManager(context)
    }
}
