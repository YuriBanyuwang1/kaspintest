package com.kasirpintar.kaspintest.data.local;

import androidx.room.TypeConverter;

import com.kasirpintar.kaspintest.data.local.entity.TxStatus;

public class Converters {

    @TypeConverter
    public static String fromStatus(TxStatus status) {
        return status == null ? null : status.name();
    }

    @TypeConverter
    public static TxStatus toStatus(String value) {
        return value == null ? null : TxStatus.valueOf(value);
    }
}
