package com.kasirpintar.kaspintest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.kasirpintar.kaspintest.data.remote.ApiService;
import com.kasirpintar.kaspintest.data.remote.NetworkToggle;
import com.kasirpintar.kaspintest.data.remote.TransactionPayload;
import com.kasirpintar.kaspintest.data.remote.TransactionResponse;
import com.kasirpintar.kaspintest.data.remote.MockApiServer;

import org.junit.After;
import org.junit.Test;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Pure JVM test (no Android framework needed) of the idempotency + force-fail
 * behaviour the mock server exists to demonstrate, per Task 4's brief.
 */
public class MockApiServerTest {

    @After
    public void tearDown() {
        NetworkToggle.setForceFail(false);
    }

    private ApiService buildClient(String baseUrl) {
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService.class);
    }

    @Test
    public void sameTxId_postedTwice_isFlaggedAsDuplicateOnSecondCall() throws Exception {
        String baseUrl = new MockApiServer().start();
        ApiService api = buildClient(baseUrl);
        TransactionPayload payload = new TransactionPayload("tx-idem-1", "P001", 1, 18000);

        Response<TransactionResponse> first = api.postTransaction(payload).execute();
        Response<TransactionResponse> second = api.postTransaction(payload).execute();

        assertTrue(first.isSuccessful());
        assertFalse(first.body().duplicate);

        assertTrue(second.isSuccessful());
        assertTrue(second.body().duplicate);
    }

    @Test
    public void forceFailToggle_makesServerReturnError() throws Exception {
        String baseUrl = new MockApiServer().start();
        ApiService api = buildClient(baseUrl);
        NetworkToggle.setForceFail(true);

        Response<TransactionResponse> response =
                api.postTransaction(new TransactionPayload("tx-fail-1", "P001", 1, 18000)).execute();

        assertEquals(503, response.code());
    }
}
