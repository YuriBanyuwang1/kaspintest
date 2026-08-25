package com.kasirpintar.kaspintest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.kasirpintar.kaspintest.data.local.AppDatabase;
import com.kasirpintar.kaspintest.data.local.entity.OutboxEntity;
import com.kasirpintar.kaspintest.data.local.entity.ProductEntity;
import com.kasirpintar.kaspintest.data.local.entity.TransactionEntity;
import com.kasirpintar.kaspintest.data.local.entity.TxStatus;
import com.kasirpintar.kaspintest.data.repository.TransactionRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;
import java.util.List;

/**
 * Covers the three things §6/Task 3 called out as risk: stock correctness,
 * outbox creation alongside the transaction, and idempotency of the client UUID.
 * Uses an in-memory Room DB via Robolectric so these run as plain JVM unit tests,
 * no emulator needed.
 */
@RunWith(RobolectricTestRunner.class)
public class TransactionRepositoryTest {

    private AppDatabase db;
    private TransactionRepository repository;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        repository = new TransactionRepository(db);

        db.productDao().insertAll(Collections.singletonList(
                new ProductEntity("P001", "Kopi Susu", 18000, 5)));
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void createTransaction_decrementsStock() throws Exception {
        repository.createTransaction("P001", 2);

        ProductEntity product = db.productDao().getById("P001");
        assertEquals(3, product.stock);
    }

    @Test
    public void createTransaction_insufficientStock_throwsAndLeavesStockUnchanged() {
        assertThrows(TransactionRepository.InsufficientStockException.class,
                () -> repository.createTransaction("P001", 10));

        ProductEntity product = db.productDao().getById("P001");
        assertEquals(5, product.stock);
    }

    @Test
    public void createTransaction_alsoCreatesPendingOutboxEntry() throws Exception {
        String txId = repository.createTransaction("P001", 1);

        TransactionEntity tx = db.transactionDao().getById(txId);
        assertEquals(TxStatus.PENDING, tx.status);

        List<OutboxEntity> retryable = db.outboxDao().getRetryable();
        assertEquals(1, retryable.size());
        assertEquals(txId, retryable.get(0).txId);
        assertEquals(TxStatus.PENDING, retryable.get(0).status);
        assertTrue(retryable.get(0).payloadJson.contains(txId));
    }

    @Test
    public void retry_afterFailure_resetsToPendingWithoutDuplicatingOutboxRow() throws Exception {
        String txId = repository.createTransaction("P001", 1);
        db.outboxDao().updateResult(txId, TxStatus.FAILED, 5, "HTTP 503", System.currentTimeMillis());

        repository.retry(txId);

        List<OutboxEntity> retryable = db.outboxDao().getRetryable();
        assertEquals(1, retryable.size());
        assertEquals(TxStatus.PENDING, retryable.get(0).status);
        assertEquals(0, retryable.get(0).retryCount);
    }
}
