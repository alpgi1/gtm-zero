package com.gtmzero.controller;

import com.gtmzero.dto.IngestDocumentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Smoke test for IngestionController using MockMvc.
 * EmbeddingModel is mocked to avoid real API calls.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class IngestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private EmbeddingModel embeddingModel;

    private static final String TEST_CONTENT = """
            # Controller Test Document

            This paragraph contains enough text to meet the 100-character minimum validation
            requirement for document ingestion through the REST API endpoint.

            Second paragraph adds more substance to ensure the chunking service has material
            to work with during the controller integration test.
            """;

    private void stubEmbeddingModel() {
        when(embeddingModel.call(any(org.springframework.ai.embedding.EmbeddingRequest.class)))
                .thenAnswer(invocation -> {
                    org.springframework.ai.embedding.EmbeddingRequest request = invocation.getArgument(0);
                    java.util.List<String> inputs = request.getInstructions();
                    java.util.List<org.springframework.ai.embedding.Embedding> embeddings =
                            new java.util.ArrayList<>();
                    for (int i = 0; i < inputs.size(); i++) {
                        float[] vec = new float[1024];
                        Arrays.fill(vec, 0.02f * (i + 1));
                        embeddings.add(new org.springframework.ai.embedding.Embedding(vec, i));
                    }
                    return new org.springframework.ai.embedding.EmbeddingResponse(embeddings);
                });
    }

    private String toJson(Object obj) throws Exception {
        return jsonMapper.writeValueAsString(obj);
    }

    @Test
    void postDocument_returns201_withValidPayload() throws Exception {
        stubEmbeddingModel();

        IngestDocumentRequest request = new IngestDocumentRequest(
                "Controller Test Doc",
                "TECHNICAL_DOC",
                "/test/doc.md",
                TEST_CONTENT
        );

        mockMvc.perform(post("/api/v1/admin/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Controller Test Doc"))
                .andExpect(jsonPath("$.chunkCount").isNumber())
                .andExpect(jsonPath("$.wasSkipped").value(false))
                .andExpect(jsonPath("$.documentId").isNotEmpty());
    }

    @Test
    void postDocument_returns400_withInvalidPayload() throws Exception {
        IngestDocumentRequest request = new IngestDocumentRequest(
                "",       // blank title
                "INVALID_TYPE",
                null,
                "too short"  // less than 100 chars
        );

        mockMvc.perform(post("/api/v1/admin/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getDocuments_returnsEmptyList_initially() throws Exception {
        mockMvc.perform(get("/api/v1/admin/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void postThenGet_returnsIngestedDocument() throws Exception {
        stubEmbeddingModel();

        IngestDocumentRequest request = new IngestDocumentRequest(
                "List Test Doc",
                "README",
                null,
                TEST_CONTENT
        );

        // Ingest
        mockMvc.perform(post("/api/v1/admin/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isCreated());

        // List
        mockMvc.perform(get("/api/v1/admin/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("List Test Doc"))
                .andExpect(jsonPath("$[0].sourceType").value("README"))
                .andExpect(jsonPath("$[0].chunkCount").isNumber());
    }

    @Test
    void deleteDocument_returns204() throws Exception {
        stubEmbeddingModel();

        IngestDocumentRequest request = new IngestDocumentRequest(
                "Delete Test Doc",
                "API_SPEC",
                null,
                TEST_CONTENT
        );

        // Ingest first
        String responseJson = mockMvc.perform(post("/api/v1/admin/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String documentId = jsonMapper.readTree(responseJson).get("documentId").asText();

        // Delete
        mockMvc.perform(delete("/api/v1/admin/documents/" + documentId))
                .andExpect(status().isNoContent());

        // Verify it's gone
        mockMvc.perform(get("/api/v1/admin/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void deleteNonexistent_returns404() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/documents/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }
}
