package com.gtmzero.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Token-approximate paragraph chunker. Splits on double-newline boundaries
 * first, then greedy-packs paragraphs into chunks up to the size limit.
 * Overlap is taken from the trailing context of the previous chunk.
 */
@Service
public class ChunkingService {

    private static final int DEFAULT_CHUNK_SIZE = 1500;
    private static final int DEFAULT_OVERLAP = 200;

    public List<String> chunk(String text) {
        return chunk(text, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
    }

    public List<String> chunk(String text, int chunkSizeChars, int overlapChars) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        // Normalize line endings and strip excessive whitespace
        String normalized = text.replaceAll("\\r\\n", "\n")
                                .replaceAll("\\r", "\n")
                                .strip();

        // Split on paragraph boundaries (double newline)
        String[] paragraphs = normalized.split("\\n\\n+");

        if (paragraphs.length <= 1) {
            // No paragraph boundaries — fall back to char-window splitting
            return charWindowChunk(normalized, chunkSizeChars, overlapChars);
        }

        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String paragraph : paragraphs) {
            String trimmed = paragraph.strip();
            if (trimmed.isEmpty()) continue;

            // Would adding this paragraph exceed the limit?
            int projectedLength = current.isEmpty()
                    ? trimmed.length()
                    : current.length() + 2 + trimmed.length(); // +2 for "\n\n"

            if (projectedLength > chunkSizeChars && !current.isEmpty()) {
                // Emit the current chunk
                chunks.add(current.toString().strip());

                // Start new chunk with overlap from previous
                String overlap = extractOverlap(current.toString(), overlapChars);
                current = new StringBuilder();
                if (!overlap.isEmpty()) {
                    current.append(overlap).append("\n\n");
                }
            }

            if (!current.isEmpty()) {
                current.append("\n\n");
            }
            current.append(trimmed);
        }

        // Emit final chunk
        if (!current.isEmpty()) {
            String finalChunk = current.toString().strip();
            if (!finalChunk.isEmpty()) {
                chunks.add(finalChunk);
            }
        }

        return chunks;
    }

    /**
     * Fallback: sliding window over raw text when no paragraph boundaries exist.
     */
    private List<String> charWindowChunk(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            String chunk = text.substring(start, end).strip();

            // Try not to cut mid-word: backtrack to last space if not at end
            if (end < text.length() && !Character.isWhitespace(text.charAt(end))) {
                int lastSpace = chunk.lastIndexOf(' ');
                if (lastSpace > chunkSize / 2) {
                    chunk = chunk.substring(0, lastSpace).strip();
                    end = start + lastSpace;
                }
            }

            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            // Advance: start next chunk with overlap from current chunk's end
            int nextStart = end - overlap;
            // Safety: must always advance by at least 1 char to avoid infinite loop
            if (nextStart <= start) {
                nextStart = end;
            }
            start = nextStart;
        }
        return chunks;
    }

    /**
     * Extracts trailing overlap text, preferring sentence boundaries.
     */
    private String extractOverlap(String text, int overlapChars) {
        if (text.length() <= overlapChars) {
            return text;
        }
        String tail = text.substring(text.length() - overlapChars);
        // Try to break at a sentence boundary (. or !)
        int sentenceEnd = Math.max(tail.indexOf(". "), tail.indexOf("! "));
        if (sentenceEnd > 0 && sentenceEnd < tail.length() - 10) {
            return tail.substring(sentenceEnd + 2).strip();
        }
        // Otherwise break at first space to avoid mid-word cut
        int firstSpace = tail.indexOf(' ');
        if (firstSpace > 0) {
            return tail.substring(firstSpace + 1).strip();
        }
        return tail.strip();
    }
}
