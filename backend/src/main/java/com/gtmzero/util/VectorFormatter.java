package com.gtmzero.util;

import java.util.Locale;

/**
 * Converts between float[] embeddings and the pgvector string format
 * "[x1,x2,...,xN]" used by {@code findTopKByEmbedding} in
 * DocumentChunkRepository. Locale.US is mandatory to avoid comma
 * decimals in locales like de_DE.
 */
public final class VectorFormatter {

    private VectorFormatter() {}

    /**
     * Converts a float[] embedding to the pgvector string format.
     * Output: "[0.123456,0.654321,...]" — no spaces, 6 decimal precision.
     */
    public static String toPgVectorString(float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            throw new IllegalArgumentException("Embedding must not be null or empty");
        }
        StringBuilder sb = new StringBuilder(embedding.length * 10);
        sb.append('[');
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(String.format(Locale.US, "%.6f", embedding[i]));
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * Parses a pgvector string "[x1,x2,...,xN]" back to float[].
     * Useful for debugging.
     */
    public static float[] fromPgVectorString(String s) {
        if (s == null || s.length() < 3) {
            throw new IllegalArgumentException("Invalid pgvector string: " + s);
        }
        String inner = s.substring(1, s.length() - 1);
        String[] parts = inner.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }
}
