package com.kasirpintar.kaspintest.sync;

import android.content.Context;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/**
 * Single entry point for enqueuing sync work. NetworkType.CONNECTED is the piece
 * that actually makes the airplane-mode demo work: WorkManager itself withholds
 * the job until the OS reports connectivity, regardless of the in-process mock
 * server being reachable on localhost the whole time.
 */
public class SyncScheduler {

    public static void requestSync(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .addTag(SyncWorker.TAG)
                .build();

        WorkManager.getInstance(context)
                .enqueueUniqueWork(SyncWorker.TAG, ExistingWorkPolicy.APPEND_OR_REPLACE, request);
    }
}
