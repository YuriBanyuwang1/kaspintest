package com.kasirpintar.kaspintest.sync;

import com.google.gson.Gson;
import com.kasirpintar.kaspintest.data.local.AppDatabase;
import com.kasirpintar.kaspintest.data.local.dao.OutboxDao;
import com.kasirpintar.kaspintest.data.local.dao.TransactionDao;
import com.kasirpintar.kaspintest.data.local.entity.OutboxEntity;
import com.kasirpintar.kaspintest.data.local.entity.TxStatus;
import com.kasirpintar.kaspintest.data.remote.ApiService;
import com.kasirpintar.kaspintest.data.remote.TransactionPayload;
import com.kasirpintar.kaspintest.data.remote.TransactionResponse;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

/**
 * The actual outbox-draining logic, kept free of Android's Worker/Application
 * plumbing so the retry state machine can be unit tested without an emulator.
 * {@link SyncWorker} is a thin wrapper that supplies the dependencies.
 */
public class OutboxSyncer {

    /** What the caller should tell WorkManager once the run finishes. */
    public enum Outcome {
        /** Nothing left that could succeed on a later attempt. */
        DONE,
        /** At least one entry failed but has retries left — ask for backoff. */
        NEEDS_RETRY
    }

    /** Attempts allowed before a queued transaction is parked as FAILED. */
    public static final int MAX_RETRY = 5;

    private final AppDatabase db;
    private final ApiService api;
    private final Gson gson;

    public OutboxSyncer(AppDatabase db, ApiService api, Gson gson) {
        this.db = db;
        this.api = api;
        this.gson = gson;
    }

    /**
     * Attempts every retryable outbox row once. Drains the whole queue rather than
     * a single item so transactions piled up while offline all catch up together.
     */
    public Outcome drainOutbox() {
        OutboxDao outboxDao = db.outboxDao();
        TransactionDao transactionDao = db.transactionDao();

        List<OutboxEntity> pending = outboxDao.getRetryable();
        boolean anyRetryable = false;

        for (OutboxEntity entry : pending) {
            TransactionPayload payload = gson.fromJson(entry.payloadJson, TransactionPayload.class);

            try {
                Call<TransactionResponse> call = api.postTransaction(payload);
                Response<TransactionResponse> response = call.execute();

                if (response.isSuccessful()) {
                    outboxDao.updateResult(entry.txId, TxStatus.SYNCED, entry.retryCount, null, System.currentTimeMillis());
                    transactionDao.updateStatus(entry.txId, TxStatus.SYNCED);
                } else {
                    anyRetryable |= handleFailure(outboxDao, transactionDao, entry, "HTTP " + response.code());
                }
            } catch (IOException e) {
                anyRetryable |= handleFailure(outboxDao, transactionDao, entry, e.getMessage());
            }
        }

        return anyRetryable ? Outcome.NEEDS_RETRY : Outcome.DONE;
    }

    /** @return true when the entry still has attempts left (i.e. worth a backoff retry). */
    private boolean handleFailure(OutboxDao outboxDao, TransactionDao transactionDao,
                                   OutboxEntity entry, String error) {
        int retryCount = entry.retryCount + 1;
        TxStatus status = retryCount >= MAX_RETRY ? TxStatus.FAILED : TxStatus.PENDING;

        outboxDao.updateResult(entry.txId, status, retryCount, error, System.currentTimeMillis());
        if (status == TxStatus.FAILED) {
            transactionDao.updateStatus(entry.txId, TxStatus.FAILED);
        }
        return status != TxStatus.FAILED;
    }
}
