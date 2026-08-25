package com.kasirpintar.kaspintest.data.remote;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    @POST("/transactions")
    Call<TransactionResponse> postTransaction(@Body TransactionPayload payload);
}
