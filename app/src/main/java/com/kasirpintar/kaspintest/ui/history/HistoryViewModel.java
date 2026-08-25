package com.kasirpintar.kaspintest.ui.history;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.kasirpintar.kaspintest.KaspinApp;
import com.kasirpintar.kaspintest.data.local.entity.TransactionEntity;

import java.util.List;

public class HistoryViewModel extends AndroidViewModel {

    private final LiveData<List<TransactionEntity>> transactions;

    public HistoryViewModel(@NonNull Application application) {
        super(application);
        KaspinApp app = (KaspinApp) application;
        transactions = app.getDatabase().transactionDao().observeAll();
    }

    public LiveData<List<TransactionEntity>> getTransactions() {
        return transactions;
    }
}
