package com.kasirpintar.kaspintest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.google.gson.Gson;
import com.kasirpintar.kaspintest.data.local.AppDatabase;
import com.kasirpintar.kaspintest.data.local.entity.OutboxEntity;
import com.kasirpintar.kaspintest.data.local.entity.TransactionEntity;
import com.kasirpintar.kaspintest.data.local.entity.TxStatus;
import com.kasirpintar.kaspintest.data.remote.ApiService;
import com.kasirpintar.kaspintest.data.remote.TransactionPayload;
import com.kasirpintar.kaspintest.sync.OutboxSyncer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Covers the retry state machine that decides when a queued transaction stays
 * PENDING (worth another backoff attempt) versus when it becomes terminally
 * FAILED and needs a manual retry from the Queue screen. This is the core of the
 * offline-first guarantee, so it is exercised against a real MockWebServer over
 * the real Retrofit stack rather than a hand-rolled fake.
 */
@RunWith(RobolectricTestRunner.class)
public class OutboxSyncerTest {

    private AppDatabase db;
    private MockWebServer server;
    private OutboxSyncer syncer;
    private final Gson gson = new Gson();

    @Before
    public void setUp() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();

        server = new MockWebServer();
        server.start();

        ApiService api = new Retrofit.Builder()
                .baseUrl(server.url("/").toString())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService.class);

        syncer = new OutboxSyncer(db, api, gson);
    }

    @After
    public void tearDown() throws Exception {
        db.close();
        server.shutdown();
    }

    /** Seeds a transaction plus its outbox row in the given starting state. */
    private void seed(String txId, TxStatus status, int retryCount) {
        db.transactionDao().insert(new TransactionEntity(
                txId, "P001", "Kopi Susu", 1, 18000, System.currentTimeMillis(), status));
        String payload = gson.toJson(new TransactionPayload(txId, "P001", 1, 18000));
        db.outboxDao().insert(new OutboxEntity(
                txId, payload, status, retryCount, null, System.currentTimeMillis()));
    }

    private MockResponse ok(String txId) {
        return new MockResponse().setResponseCode(200)
                .setBody("{\"txId\":\"" + txId + "\",\"serverStatus\":\"ACCEPTED\",\"duplicate\":false}");
    }

    private OutboxEntity outbox(String txId) {
        for (OutboxEntity e : db.outboxDao().getRetryable()) {
            if (e.txId.equals(txId)) return e;
        }
        return null;
    }

    @Test
    public void successfulPost_marksBothOutboxAndTransactionSynced() {
        seed("tx-ok", TxStatus.PENDING, 0);
        server.enqueue(ok("tx-ok"));

        OutboxSyncer.Outcome outcome = syncer.drainOutbox();

        assertEquals(OutboxSyncer.Outcome.DONE, outcome);
        assertEquals(TxStatus.SYNCED, db.transactionDao().getById("tx-ok").status);
        // SYNCED rows drop out of the retryable set entirely.
        assertNull(outbox("tx-ok"));
    }

    @Test
    public void serverError_belowMaxRetry_staysPendingAndRecordsAttempt() {
        seed("tx-503", TxStatus.PENDING, 0);
        server.enqueue(new MockResponse().setResponseCode(503));

        OutboxSyncer.Outcome outcome = syncer.drainOutbox();

        assertEquals(OutboxSyncer.Outcome.NEEDS_RETRY, outcome);

        OutboxEntity entry = outbox("tx-503");
        assertNotNull(entry);
        assertEquals(TxStatus.PENDING, entry.status);
        assertEquals(1, entry.retryCount);
        assertEquals("HTTP 503", entry.lastError);
        // The transaction itself must not be marked FAILED while retries remain.
        assertEquals(TxStatus.PENDING, db.transactionDao().getById("tx-503").status);
    }

    @Test
    public void serverError_onFinalAttempt_marksFailedAndStopsAskingForRetry() {
        // Already used 4 attempts; this run is the 5th and last.
        seed("tx-dead", TxStatus.PENDING, OutboxSyncer.MAX_RETRY - 1);
        server.enqueue(new MockResponse().setResponseCode(503));

        OutboxSyncer.Outcome outcome = syncer.drainOutbox();

        // No backoff requested: nothing here can succeed without manual intervention.
        assertEquals(OutboxSyncer.Outcome.DONE, outcome);

        OutboxEntity entry = outbox("tx-dead");
        assertNotNull(entry);
        assertEquals(TxStatus.FAILED, entry.status);
        assertEquals(OutboxSyncer.MAX_RETRY, entry.retryCount);
        assertEquals(TxStatus.FAILED, db.transactionDao().getById("tx-dead").status);
    }

    @Test
    public void networkError_isRecordedAsRetryableFailureWithMessage() {
        seed("tx-offline", TxStatus.PENDING, 0);
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

        OutboxSyncer.Outcome outcome = syncer.drainOutbox();

        assertEquals(OutboxSyncer.Outcome.NEEDS_RETRY, outcome);

        OutboxEntity entry = outbox("tx-offline");
        assertNotNull(entry);
        assertEquals(TxStatus.PENDING, entry.status);
        assertEquals(1, entry.retryCount);
        // The real IOException message is surfaced, not swallowed — this is what
        // made the cleartext-policy bug visible on the Queue screen.
        assertNotNull(entry.lastError);
    }

    @Test
    public void drainsEveryQueuedEntryInASingleRun() {
        seed("tx-a", TxStatus.PENDING, 0);
        seed("tx-b", TxStatus.PENDING, 0);
        seed("tx-c", TxStatus.PENDING, 0);
        server.enqueue(ok("tx-a"));
        server.enqueue(ok("tx-b"));
        server.enqueue(ok("tx-c"));

        OutboxSyncer.Outcome outcome = syncer.drainOutbox();

        assertEquals(OutboxSyncer.Outcome.DONE, outcome);
        assertEquals(3, server.getRequestCount());
        assertTrue(db.outboxDao().getRetryable().isEmpty());
        for (String id : new String[]{"tx-a", "tx-b", "tx-c"}) {
            assertEquals(TxStatus.SYNCED, db.transactionDao().getById(id).status);
        }
    }

    @Test
    public void oneFailureAmongManyStillLetsTheRestThrough() {
        seed("tx-good", TxStatus.PENDING, 0);
        seed("tx-bad", TxStatus.PENDING, 0);
        // Queue order is by updatedAt ASC, i.e. insertion order here.
        server.enqueue(ok("tx-good"));
        server.enqueue(new MockResponse().setResponseCode(500));

        OutboxSyncer.Outcome outcome = syncer.drainOutbox();

        assertEquals(OutboxSyncer.Outcome.NEEDS_RETRY, outcome);
        assertEquals(TxStatus.SYNCED, db.transactionDao().getById("tx-good").status);

        OutboxEntity bad = outbox("tx-bad");
        assertNotNull(bad);
        assertEquals(TxStatus.PENDING, bad.status);
        assertEquals("HTTP 500", bad.lastError);
    }

    @Test
    public void alreadyFailedEntry_canStillSelfHealOnALaterRun() {
        seed("tx-revived", TxStatus.FAILED, OutboxSyncer.MAX_RETRY);
        server.enqueue(ok("tx-revived"));

        OutboxSyncer.Outcome outcome = syncer.drainOutbox();

        assertEquals(OutboxSyncer.Outcome.DONE, outcome);
        assertEquals(TxStatus.SYNCED, db.transactionDao().getById("tx-revived").status);
        assertNull(outbox("tx-revived"));
    }

    @Test
    public void emptyQueue_doesNotCallTheApiAtAll() {
        OutboxSyncer.Outcome outcome = syncer.drainOutbox();

        assertEquals(OutboxSyncer.Outcome.DONE, outcome);
        assertEquals(0, server.getRequestCount());
    }
}
