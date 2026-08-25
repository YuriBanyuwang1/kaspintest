package com.kasirpintar.kaspintest.data.remote;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Stands in for the real backend (brief requires a Mock API + a way to toggle
 * network failure). Runs in-process on localhost so it survives airplane mode —
 * the actual offline behaviour is exercised through WorkManager's NetworkType
 * .CONNECTED constraint, which airplane mode genuinely blocks; this server only
 * needs to fake what the real API would do once that constraint is satisfied.
 *
 * Dedupes by txId (the client-generated idempotency key) so a WorkManager retry
 * that lands after a "successful but response lost" call doesn't double-process
 * the same transaction server-side.
 */
public class MockApiServer {

    private final MockWebServer server = new MockWebServer();
    private final Gson gson = new Gson();
    private final Set<String> processedTxIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public String start() {
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if (NetworkToggle.isForceFail()) {
                    return new MockResponse().setResponseCode(503).setBody("{\"error\":\"forced_failure\"}");
                }

                TransactionPayload payload = gson.fromJson(request.getBody().readUtf8(), TransactionPayload.class);
                boolean duplicate = !processedTxIds.add(payload.txId);
                TransactionResponse response = new TransactionResponse(payload.txId, "ACCEPTED", duplicate);

                return new MockResponse()
                        .setResponseCode(200)
                        .setBody(gson.toJson(response));
            }
        });

        try {
            server.start();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start in-process mock API", e);
        }
        return server.url("/").toString();
    }
}
