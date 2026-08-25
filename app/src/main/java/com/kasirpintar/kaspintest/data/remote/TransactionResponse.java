package com.kasirpintar.kaspintest.data.remote;

public class TransactionResponse {
    public String txId;
    public String serverStatus;
    public boolean duplicate;

    public TransactionResponse(String txId, String serverStatus, boolean duplicate) {
        this.txId = txId;
        this.serverStatus = serverStatus;
        this.duplicate = duplicate;
    }
}
