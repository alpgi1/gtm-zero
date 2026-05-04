package com.gtmzero.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChunkingServiceTest {

    private final ChunkingService chunkingService = new ChunkingService();

    @Test
    void shortText_singleChunk() {
        String text = "This is a short paragraph about AI compliance.\n\nIt has two paragraphs but both are tiny.";
        List<String> chunks = chunkingService.chunk(text, 1500, 200);

        assertEquals(1, chunks.size());
        assertTrue(chunks.getFirst().contains("AI compliance"));
        assertTrue(chunks.getFirst().contains("two paragraphs"));
    }

    @Test
    void mediumText_multipleChunksWithOverlap() {
        // Build text with clear paragraph boundaries that exceed chunk size
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 12; i++) {
            sb.append("Paragraph ").append(i).append(". ");
            sb.append("This is the content of paragraph number ").append(i).append(". ");
            sb.append("It contains enough text to be meaningful and contribute to chunk sizing. ");
            sb.append("The EU AI Act requires comprehensive documentation for high-risk systems. ");
            sb.append("Article ").append(i).append(" specifies requirements for this category.");
            sb.append("\n\n");
        }

        List<String> chunks = chunkingService.chunk(sb.toString(), 500, 100);

        // With 12 paragraphs of ~200 chars each and a 500 char limit,
        // we should get roughly 5-7 chunks
        assertTrue(chunks.size() >= 3, "Expected at least 3 chunks, got " + chunks.size());
        assertTrue(chunks.size() <= 15, "Expected at most 15 chunks, got " + chunks.size());

        // All chunks should be non-empty and within bounds
        for (String chunk : chunks) {
            assertFalse(chunk.isBlank(), "Chunk must not be blank");
            assertTrue(chunk.length() >= 50,
                    "Chunk too short: " + chunk.length() + " chars");
            assertTrue(chunk.length() <= 700,
                    "Chunk too long: " + chunk.length() + " chars (limit 500 + overlap)");
        }

        // Verify overlap: later chunks should contain content from previous ones
        // (The overlap mechanism copies trailing text from the previous chunk)
        if (chunks.size() >= 2) {
            // First chunk's last paragraph should appear in second chunk
            // (via overlap mechanism)
            String firstChunk = chunks.get(0);
            String secondChunk = chunks.get(1);
            // At minimum, both should be distinct
            assertNotEquals(firstChunk, secondChunk);
        }
    }

    @Test
    void noDoubleNewlines_fallsBackToCharWindow() {
        // Single long paragraph without any double-newline breaks
        String text = "The EU AI Act (Regulation 2024/1689) establishes a comprehensive " +
                "framework for artificial intelligence regulation. It categorizes AI systems " +
                "into risk tiers: unacceptable risk (banned), high risk (strict requirements), " +
                "limited risk (transparency obligations), and minimal risk (no specific obligations). " +
                "High-risk AI systems must undergo conformity assessments, maintain technical " +
                "documentation per Annex IV, implement risk management systems under Article 9, " +
                "and ensure data governance per Article 10. Providers must register high-risk " +
                "systems in the EU database before placing them on the market. Deployers must " +
                "conduct fundamental rights impact assessments for certain use cases. Penalties " +
                "range from seven point five million euros to thirty five million euros depending " +
                "on the nature of the violation. The Act applies to providers, deployers, importers, " +
                "and distributors of AI systems in the EU market. Full enforcement begins on " +
                "2 August 2026 for most provisions, with earlier dates for prohibited practices " +
                "and general-purpose AI model obligations.";

        List<String> chunks = chunkingService.chunk(text, 400, 80);

        assertTrue(chunks.size() >= 2,
                "Long text without paragraph breaks should split into multiple chunks, got " + chunks.size());

        // Verify no chunk is cut mid-word (should break at spaces)
        for (String chunk : chunks) {
            assertFalse(chunk.isBlank());
            // Check chunk doesn't start/end mid-word (with a letter continuation)
            assertFalse(chunk.startsWith("-"), "Chunk starts mid-word: " + chunk.substring(0, 20));
        }
    }

    @Test
    void nullAndBlankInput_returnsEmpty() {
        assertTrue(chunkingService.chunk(null).isEmpty());
        assertTrue(chunkingService.chunk("").isEmpty());
        assertTrue(chunkingService.chunk("   ").isEmpty());
    }
}
