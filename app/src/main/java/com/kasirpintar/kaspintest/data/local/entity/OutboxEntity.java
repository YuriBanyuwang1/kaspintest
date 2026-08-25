package com.kasirpintar.kaspintest.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * One row per transaction waiting to be pushed to the server. Kept separate from
 * TransactionEntity so retry bookkeeping (retryCount / lastError) doesn't pollute
 * the transaction record itself, and the queue screen has a dedicated source.
 */
@Entity(tableName = "outbox", indices = {@Index(value = "txId", unique = true)})
public class OutboxEntity {

    @PrimaryKey(autoGenerate = true)
    public long outboxId;

    @NonNull
    public String txId;

    public String payloadJson;
    public TxStatus status;
    public int retryCount;
    public String lastError;
    public long updatedAt;

    public OutboxEntity(@NonNull String txId, String payloadJson, TxStatus status,
                         int retryCount, String lastError, long updatedAt) {
        this.txId = txId;
        this.payloadJson = payloadJson;
        this.status = status;
        this.retryCount = retryCount;
        this.lastError = lastError;
        this.updatedAt = updatedAt;
    }
}
