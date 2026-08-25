package com.kasirpintar.kaspintest.ui.queue;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.kasirpintar.kaspintest.KaspinApp;
import com.kasirpintar.kaspintest.data.local.entity.OutboxEntity;
import com.kasirpintar.kaspintest.data.remote.NetworkToggle;
import com.kasirpintar.kaspintest.data.repository.TransactionRepository;
import com.kasirpintar.kaspintest.sync.SyncScheduler;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QueueViewModel extends AndroidViewModel {

    private final TransactionRepository repository;
    private final LiveData<List<OutboxEntity>> outbox;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public QueueViewModel(@NonNull Application application) {
        super(application);
        KaspinApp app = (KaspinApp) application;
        repository = new TransactionRepository(app.getDatabase());
        outbox = app.getDatabase().outboxDao().observeAll();
    }

    public LiveData<List<OutboxEntity>> getOutbox() {
        return outbox;
    }

    public boolean isForceFailEnabled() {
        return NetworkToggle.isForceFail();
    }

    public void setForceFail(boolean enabled) {
        NetworkToggle.setForceFail(enabled);
    }

    public void retry(String txId) {
        executor.execute(() -> {
            repository.retry(txId);
            SyncScheduler.requestSync(getApplication());
        });
    }
}
