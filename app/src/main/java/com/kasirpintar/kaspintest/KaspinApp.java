package com.kasirpintar.kaspintest;

import android.app.Application;

import com.google.gson.Gson;
import com.kasirpintar.kaspintest.data.local.AppDatabase;
import com.kasirpintar.kaspintest.data.local.entity.ProductEntity;
import com.kasirpintar.kaspintest.data.remote.ApiService;
import com.kasirpintar.kaspintest.data.remote.MockApiServer;

import java.util.Arrays;
import java.util.concurrent.Executors;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class KaspinApp extends Application {

    private AppDatabase database;
    private ApiService apiService;
    private final Gson gson = new Gson();

    @Override
    public void onCreate() {
        super.onCreate();
        database = AppDatabase.getInstance(this);

        // MockWebServer must start on a background thread; startup work then
        // continues there so app launch never blocks on it.
        Executors.newSingleThreadExecutor().execute(() -> {
            String baseUrl = new MockApiServer().start();
            apiService = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(ApiService.class);

            seedProductsIfEmpty();
        });
    }

    private void seedProductsIfEmpty() {
        if (database.productDao().count() > 0) {
            return;
        }
        database.productDao().insertAll(Arrays.asList(
                new ProductEntity("P001", "Kopi Susu", 18000, 50),
                new ProductEntity("P002", "Roti Bakar", 15000, 30),
                new ProductEntity("P003", "Es Teh Manis", 8000, 100),
                new ProductEntity("P004", "Nasi Goreng", 22000, 20),
                new ProductEntity("P005", "Air Mineral", 5000, 200)
        ));
    }

    public AppDatabase getDatabase() {
        return database;
    }

    /** May briefly be null right after process start; the mock server init runs async. */
    public ApiService getApiService() {
        return apiService;
    }

    public Gson getGson() {
        return gson;
    }
}
