package com.kasirpintar.kaspintest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.kasirpintar.kaspintest.util.Formats;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Formatting must follow Indonesian convention regardless of what locale the
 * device happens to be set to — the whole point of Formats is to not inherit
 * Locale.getDefault(). These tests deliberately force a US default first.
 */
public class FormatsTest {

    private Locale originalLocale;

    @Before
    public void setUp() {
        originalLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @After
    public void tearDown() {
        Locale.setDefault(originalLocale);
    }

    @Test
    public void rupiah_usesDotAsThousandsSeparator_evenOnAUsDevice() {
        assertEquals("Rp18.000", Formats.rupiah(18000));
        assertEquals("Rp5.000", Formats.rupiah(5000));
    }

    @Test
    public void rupiah_handlesMillionsAndSmallAmounts() {
        assertEquals("Rp1.250.000", Formats.rupiah(1250000));
        assertEquals("Rp500", Formats.rupiah(500));
        assertEquals("Rp0", Formats.rupiah(0));
    }

    @Test
    public void dateTime_rendersDayMonthYearAndClockTime() {
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        cal.set(2026, Calendar.AUGUST, 26, 9, 24, 0);
        cal.set(Calendar.MILLISECOND, 0);

        String formatted = Formats.dateTime(cal.getTimeInMillis());

        assertTrue("expected the day number, got: " + formatted, formatted.startsWith("26 "));
        assertTrue("expected 24-hour clock time, got: " + formatted, formatted.endsWith("09:24"));
        assertTrue("expected the year, got: " + formatted, formatted.contains("2026"));
    }

    @Test
    public void quantityAndStock_areRenderedForDisplay() {
        assertEquals("2x", Formats.quantity(2));
        assertEquals("Stok: 50", Formats.stock(50));
        assertEquals("Stok: 0", Formats.stock(0));
    }
}
