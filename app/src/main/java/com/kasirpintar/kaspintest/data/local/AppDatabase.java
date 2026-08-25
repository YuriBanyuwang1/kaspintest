package com.kasirpintar.kaspintest.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.kasirpintar.kaspintest.data.local.dao.OutboxDao;
import com.kasirpintar.kaspintest.data.local.dao.ProductDao;
import com.kasirpintar.kaspintest.data.local.dao.TransactionDao;
import com.kasirpintar.kaspintest.data.local.entity.OutboxEntity;
import com.kasirpintar.kaspintest.data.local.entity.ProductEntity;
import com.kasirpintar.kaspintest.data.local.entity.TransactionEntity;

@Database(
        entities = {ProductEntity.class, TransactionEntity.class, OutboxEntity.class},
        version = 1,
        exportSchema = false
)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {

    public abstract ProductDao productDao();

    public abstract TransactionDao transactionDao();

    public abstract OutboxDao outboxDao();

    private static volatile AppDatabase instance;

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "kaspintest.db"
                    ).build();
                }
            }
        }
        return instance;
    }
}
