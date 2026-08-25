package com.kasirpintar.kaspintest.ui.product;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.kasirpintar.kaspintest.KaspinApp;
import com.kasirpintar.kaspintest.data.local.entity.ProductEntity;
import com.kasirpintar.kaspintest.data.repository.TransactionRepository;
import com.kasirpintar.kaspintest.sync.SyncScheduler;
import com.kasirpintar.kaspintest.ui.common.Event;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductViewModel extends AndroidViewModel {

    private final TransactionRepository repository;
    private final LiveData<List<ProductEntity>> products;
    private final MutableLiveData<Event<String>> message = new MutableLiveData<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ProductViewModel(@NonNull Application application) {
        super(application);
        KaspinApp app = (KaspinApp) application;
        repository = new TransactionRepository(app.getDatabase());
        products = app.getDatabase().productDao().observeAll();
    }

    public LiveData<List<ProductEntity>> getProducts() {
        return products;
    }

    public LiveData<Event<String>> getMessage() {
        return message;
    }

    public void createTransaction(String productId, int qty) {
        executor.execute(() -> {
            try {
                repository.createTransaction(productId, qty);
                SyncScheduler.requestSync(getApplication());
                message.postValue(new Event<>("Transaksi dibuat, menunggu sync"));
            } catch (TransactionRepository.InsufficientStockException e) {
                message.postValue(new Event<>("Stok tidak cukup"));
            }
        });
    }
}
