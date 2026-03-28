package com.polyinsights.nfccloner.data.db

import androidx.room.TypeConverter
import com.polyinsights.nfccloner.data.model.CardType

class Converters {

    @TypeConverter
    fun fromCardType(value: CardType): String = value.name

    @TypeConverter
    fun toCardType(value: String): CardType = CardType.valueOf(value)
}
