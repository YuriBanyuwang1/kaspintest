package com.kasirpintar.kaspintest.data.remote;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Debug-only switch surfaced on the Queue screen so a reviewer can force the mock
 * server to fail sync calls on demand, without needing real airplane mode, to see
 * the FAILED / retry path in the queue UI.
 */
public class NetworkToggle {

    private static final AtomicBoolean forceFail = new AtomicBoolean(false);

    public static boolean isForceFail() {
        return forceFail.get();
    }

    public static void setForceFail(boolean value) {
        forceFail.set(value);
    }
}
