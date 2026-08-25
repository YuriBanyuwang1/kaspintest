package com.kasirpintar.kaspintest.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.kasirpintar.kaspintest.data.local.entity.OutboxEntity;
import com.kasirpintar.kaspintest.data.local.entity.TxStatus;

import java.util.List;

@Dao
public interface OutboxDao {

    /**
     * IGNORE on the unique txId index: re-queuing the same transaction (e.g. a
     * retried repository call) never creates a duplicate outbox row.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(OutboxEntity entry);

    @Query("SELECT * FROM outbox WHERE status IN ('PENDING', 'FAILED') ORDER BY updatedAt ASC")
    List<OutboxEntity> getRetryable();

    @Query("SELECT * FROM outbox ORDER BY updatedAt DESC")
    LiveData<List<OutboxEntity>> observeAll();

    @Query("UPDATE outbox SET status = :status, retryCount = :retryCount, lastError = :lastError, updatedAt = :updatedAt WHERE txId = :txId")
    void updateResult(String txId, TxStatus status, int retryCount, String lastError, long updatedAt);

    @Query("UPDATE outbox SET status = 'PENDING', retryCount = 0, lastError = NULL, updatedAt = :updatedAt WHERE txId = :txId")
    void resetForRetry(String txId, long updatedAt);

    @Query("SELECT COUNT(*) FROM outbox WHERE status = 'FAILED'")
    int countFailed();
}
