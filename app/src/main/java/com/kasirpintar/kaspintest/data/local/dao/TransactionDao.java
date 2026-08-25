package com.kasirpintar.kaspintest.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.kasirpintar.kaspintest.data.local.entity.TransactionEntity;
import com.kasirpintar.kaspintest.data.local.entity.TxStatus;

import java.util.List;

@Dao
public interface TransactionDao {

    @Insert
    void insert(TransactionEntity transaction);

    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    LiveData<List<TransactionEntity>> observeAll();

    @Query("SELECT * FROM transactions WHERE txId = :txId")
    TransactionEntity getById(String txId);

    @Query("UPDATE transactions SET status = :status WHERE txId = :txId")
    void updateStatus(String txId, TxStatus status);

    @Query("SELECT COUNT(*) FROM transactions")
    int count();
}
