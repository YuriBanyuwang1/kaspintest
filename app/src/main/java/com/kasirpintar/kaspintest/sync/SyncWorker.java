package com.kasirpintar.kaspintest.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;
import androidx.work.Worker;

import com.kasirpintar.kaspintest.KaspinApp;
import com.kasirpintar.kaspintest.data.remote.ApiService;

/**
 * Thin WorkManager wrapper: resolves dependencies from the Application and hands
 * the real work to {@link OutboxSyncer}, which is where the retry state machine
 * lives (and where it is unit tested).
 *
 * A row is only marked terminally FAILED after OutboxSyncer.MAX_RETRY attempts;
 * before that it stays PENDING and this Worker returns retry() so WorkManager's
 * exponential backoff re-runs it. This mirrors the pipeline-gate lesson from the
 * printer/ProGuard incident: failures should surface as retryable state, not
 * silent loss.
 */
public class SyncWorker extends Worker {

    public static final String TAG = "sync_queue";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        KaspinApp app = (KaspinApp) getApplicationContext();
        ApiService api = app.getApiService();
        if (api == null) {
            // Mock server still starting up (async in KaspinApp#onCreate); try again shortly.
            return Result.retry();
        }

        OutboxSyncer syncer = new OutboxSyncer(app.getDatabase(), api, app.getGson());
        return syncer.drainOutbox() == OutboxSyncer.Outcome.NEEDS_RETRY
                ? Result.retry()
                : Result.success();
    }
}
