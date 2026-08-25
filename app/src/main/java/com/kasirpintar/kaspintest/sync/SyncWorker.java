package com.kasirpintar.kaspintest.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;
import androidx.work.Worker;

import com.kasirpintar.kaspintest.KaspinApp;
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
 * Drains the whole outbox on every run (not just the item that triggered it),
 * so a batch of transactions made while offline all catch up together once the
 * NetworkType.CONNECTED constraint (set where this Worker is enqueued) is met.
 *
 * A row is only marked terminally FAILED after MAX_RETRY attempts; before that
 * it stays PENDING and the Worker returns retry() so WorkManager's exponential
 * backoff re-runs it. This mirrors the pipeline-gate lesson from the printer/
 * ProGuard incident: failures should surface as retryable state, not silent loss.
 */
public class SyncWorker extends Worker {

    public static final String TAG = "sync_queue";
    private static final int MAX_RETRY = 5;

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        KaspinApp app = (KaspinApp) getApplicationContext();
        AppDatabase db = app.getDatabase();
        ApiService api = app.getApiService();
        if (api == null) {
            // Mock server still starting up (async in KaspinApp#onCreate); try again shortly.
            return Result.retry();
        }

        OutboxDao outboxDao = db.outboxDao();
        TransactionDao transactionDao = db.transactionDao();

        List<OutboxEntity> pending = outboxDao.getRetryable();
        boolean anyFailure = false;

        for (OutboxEntity entry : pending) {
            TransactionPayload payload = app.getGson().fromJson(entry.payloadJson, TransactionPayload.class);

            try {
                Call<TransactionResponse> call = api.postTransaction(payload);
                Response<TransactionResponse> response = call.execute();

                if (response.isSuccessful()) {
                    outboxDao.updateResult(entry.txId, TxStatus.SYNCED, entry.retryCount, null, System.currentTimeMillis());
                    transactionDao.updateStatus(entry.txId, TxStatus.SYNCED);
                } else {
                    anyFailure |= handleFailure(outboxDao, transactionDao, entry, "HTTP " + response.code());
                }
            } catch (IOException e) {
                anyFailure |= handleFailure(outboxDao, transactionDao, entry, e.getMessage());
            }
        }

        return anyFailure ? Result.retry() : Result.success();
    }

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
