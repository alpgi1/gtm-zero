package com.gtmzero.util;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class VectorFormatterTest {

    @Test
    void toPgVectorString_producesCorrectFormat() {
        float[] embedding = {0.1f, 0.2f, 0.3f};
        String result = VectorFormatter.toPgVectorString(embedding);
        assertEquals("[0.100000,0.200000,0.300000]", result);
    }

    @Test
    void roundTrip_preservesPrecision() {
        float[] original = new float[1024];
        for (int i = 0; i < original.length; i++) {
            original[i] = (float) (Math.random() * 2 - 1); // [-1, 1]
        }

        String pgString = VectorFormatter.toPgVectorString(original);
        float[] parsed = VectorFormatter.fromPgVectorString(pgString);

        assertEquals(original.length, parsed.length);
        for (int i = 0; i < original.length; i++) {
            // 6 decimal places precision: tolerance of 1e-5
            assertEquals(original[i], parsed[i], 1e-5f,
                    "Mismatch at index " + i);
        }
    }

    @Test
    void localeSafety_noCommaDecimals() {
        // Simulate a German locale context — VectorFormatter must still use dots
        Locale defaultLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            float[] embedding = {1.5f, -0.333f};
            String result = VectorFormatter.toPgVectorString(embedding);

            // Must NOT contain comma-separated decimals like "1,500000"
            assertFalse(result.contains(" "), "Must not contain spaces");
            assertTrue(result.startsWith("["), "Must start with [");
            assertTrue(result.endsWith("]"), "Must end with ]");
            // Parse back to verify correctness
            float[] parsed = VectorFormatter.fromPgVectorString(result);
            assertEquals(1.5f, parsed[0], 1e-5f);
            assertEquals(-0.333f, parsed[1], 1e-3f);
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }

    @Test
    void toPgVectorString_throwsOnNull() {
        assertThrows(IllegalArgumentException.class,
                () -> VectorFormatter.toPgVectorString(null));
    }

    @Test
    void toPgVectorString_throwsOnEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> VectorFormatter.toPgVectorString(new float[0]));
    }

    @Test
    void fromPgVectorString_throwsOnInvalid() {
        assertThrows(IllegalArgumentException.class,
                () -> VectorFormatter.fromPgVectorString(null));
        assertThrows(IllegalArgumentException.class,
                () -> VectorFormatter.fromPgVectorString(""));
    }
}
