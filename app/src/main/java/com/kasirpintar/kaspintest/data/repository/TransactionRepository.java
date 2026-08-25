package com.kasirpintar.kaspintest.data.repository;

import com.google.gson.Gson;
import com.kasirpintar.kaspintest.data.local.AppDatabase;
import com.kasirpintar.kaspintest.data.local.dao.OutboxDao;
import com.kasirpintar.kaspintest.data.local.dao.ProductDao;
import com.kasirpintar.kaspintest.data.local.dao.TransactionDao;
import com.kasirpintar.kaspintest.data.local.entity.OutboxEntity;
import com.kasirpintar.kaspintest.data.local.entity.ProductEntity;
import com.kasirpintar.kaspintest.data.local.entity.TransactionEntity;
import com.kasirpintar.kaspintest.data.local.entity.TxStatus;
import com.kasirpintar.kaspintest.data.remote.TransactionPayload;

import java.util.UUID;

public class TransactionRepository {

    public static class InsufficientStockException extends Exception {
    }

    private final AppDatabase db;
    private final ProductDao productDao;
    private final TransactionDao transactionDao;
    private final OutboxDao outboxDao;
    private final Gson gson = new Gson();

    public TransactionRepository(AppDatabase db) {
        this.db = db;
        this.productDao = db.productDao();
        this.transactionDao = db.transactionDao();
        this.outboxDao = db.outboxDao();
    }

    /**
     * Decrements stock, records the transaction as PENDING and queues it for sync —
     * all inside one Room transaction so a crash mid-way never leaves stock
     * decremented without a matching transaction/outbox row, or vice versa.
     *
     * @return the new transaction's client-generated UUID (also the idempotency key).
     */
    public String createTransaction(String productId, int qty) throws InsufficientStockException {
        String txId = UUID.randomUUID().toString();

        try {
            db.runInTransaction(() -> {
                ProductEntity product = productDao.getById(productId);
                if (product == null) {
                    throw new IllegalArgumentException("Unknown product: " + productId);
                }

                int rowsUpdated = productDao.decrementStock(productId, qty);
                if (rowsUpdated == 0) {
                    throw new RuntimeException(new InsufficientStockException());
                }

                long now = System.currentTimeMillis();
                long total = product.price * qty;

                transactionDao.insert(new TransactionEntity(
                        txId, productId, product.name, qty, total, now, TxStatus.PENDING));

                String payloadJson = gson.toJson(new TransactionPayload(txId, productId, qty, total));
                outboxDao.insert(new OutboxEntity(txId, payloadJson, TxStatus.PENDING, 0, null, now));
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof InsufficientStockException) {
                throw (InsufficientStockException) e.getCause();
            }
            throw e;
        }

        return txId;
    }

    public void retry(String txId) {
        outboxDao.resetForRetry(txId, System.currentTimeMillis());
        transactionDao.updateStatus(txId, TxStatus.PENDING);
    }
}
