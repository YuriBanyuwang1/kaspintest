package com.kasirpintar.kaspintest.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * txId is a client-generated UUID: primary key locally AND the idempotency key
 * sent to the (mock) API, so a retried sync never double-counts server-side.
 */
@Entity(tableName = "transactions")
public class TransactionEntity {

    @PrimaryKey
    @NonNull
    public String txId;

    public String productId;
    public String productName;
    public int qty;
    public long totalPrice;
    public long createdAt;
    public TxStatus status;

    public TransactionEntity(@NonNull String txId, String productId, String productName,
                              int qty, long totalPrice, long createdAt, TxStatus status) {
        this.txId = txId;
        this.productId = productId;
        this.productName = productName;
        this.qty = qty;
        this.totalPrice = totalPrice;
        this.createdAt = createdAt;
        this.status = status;
    }
}
