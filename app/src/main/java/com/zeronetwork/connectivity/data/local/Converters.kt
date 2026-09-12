package com.zeronetwork.connectivity.data.local

import androidx.room.TypeConverter
import com.zeronetwork.connectivity.data.model.MessageStatus
import com.zeronetwork.connectivity.data.model.MessageType

class Converters {
    @TypeConverter
    fun fromMessageType(value: MessageType): String = value.name

    @TypeConverter
    fun toMessageType(value: String): MessageType = try {
        MessageType.valueOf(value)
    } catch (e: Exception) {
        MessageType.TEXT
    }

    @TypeConverter
    fun fromMessageStatus(value: MessageStatus): String = value.name

    @TypeConverter
    fun toMessageStatus(value: String): MessageStatus = try {
        MessageStatus.valueOf(value)
    } catch (e: Exception) {
        MessageStatus.SENT
    }
}
