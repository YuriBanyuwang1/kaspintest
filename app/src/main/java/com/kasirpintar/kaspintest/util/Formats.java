package com.kasirpintar.kaspintest.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Display formatting pinned to Indonesian conventions rather than the device
 * locale. A POS used by Indonesian merchants must show Rp18.000 (dot as the
 * thousands separator); relying on Locale.getDefault() renders Rp18,000 on any
 * device set to en-US, which reads as a different amount entirely to the cashier.
 */
public final class Formats {

    /** Java's legacy language code for Indonesian is "in", not "id". */
    private static final Locale ID = new Locale("in", "ID");

    private Formats() {
    }

    /** e.g. 18000 -> "Rp18.000" */
    public static String rupiah(long amount) {
        return String.format(ID, "Rp%,d", amount);
    }

    /** e.g. "26 Agu 2026, 09:24" */
    public static String dateTime(long epochMillis) {
        return new SimpleDateFormat("d MMM yyyy, HH:mm", ID).format(new Date(epochMillis));
    }

    /** e.g. 2 -> "2x" */
    public static String quantity(int qty) {
        return String.format(ID, "%dx", qty);
    }

    /** e.g. 50 -> "Stok: 50" */
    public static String stock(int stock) {
        return String.format(ID, "Stok: %d", stock);
    }
}
