package com.kasirpintar.kaspintest.data.remote;

/** Wire model sent to the (mock) API. txId doubles as the idempotency key. */
public class TransactionPayload {
    public String txId;
    public String productId;
    public int qty;
    public long totalPrice;

    public TransactionPayload(String txId, String productId, int qty, long totalPrice) {
        this.txId = txId;
        this.productId = productId;
        this.qty = qty;
        this.totalPrice = totalPrice;
    }
}
