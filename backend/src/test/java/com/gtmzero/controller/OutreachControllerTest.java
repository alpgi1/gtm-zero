package com.gtmzero.controller;

import com.gtmzero.dto.outreach.GenerateOutreachRequest;
import com.gtmzero.repository.AuditLogRepository;
import com.gtmzero.repository.OutreachMessageRepository;
import com.gtmzero.repository.ProspectRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestPropertySource(properties = "app.outreach-seed.enabled=false")
class OutreachControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private OutreachMessageRepository outreachMessageRepository;
    @Autowired private ProspectRepository prospectRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    @MockitoBean private ChatModel chatModel;

    private static final String VALID_JSON = """
            {
              "subject": "AI Act exposure check",
              "body": "Hi Marie,\\n\\nNoticed Lumeon Health is heading into FDA review. \
            EU AI Act enforcement opens August 2026 — clinical decision-support \
            sits squarely in the high-risk tier. Regu does first-pass risk \
            classification with article-level citations so you can sanity-check \
            scope before bringing in outside counsel.\\n\\n15-minute call next \
            Tuesday, or one line back if you're already on top of it?\\n\\nAlpgiray",
              "personalization_basis": "Used the FDA submission contextNote and VP Eng role."
            }
            """;

    @BeforeEach
    void setUp() {
        cleanup();
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse(VALID_JSON));
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        outreachMessageRepository.deleteAll();
        prospectRepository.deleteAll();
        auditLogRepository.findAllByEventTypeOrderByCreatedAtDesc("OUTREACH_GENERATED")
                .forEach(auditLogRepository::delete);
        auditLogRepository.findAllByEventTypeOrderByCreatedAtDesc("OUTREACH_SENT_MOCK")
                .forEach(auditLogRepository::delete);
    }

    @Test
    void postGenerate_returns201WithResponseBody() throws Exception {
        GenerateOutreachRequest req = new GenerateOutreachRequest(
                "Marie Dubois",
                "VP of Engineering",
                "Lumeon Health",
                "lumeon.health",
                "https://linkedin.com/in/marie-dubois",
                null,
                List.of("Python", "PyTorch"),
                "FDA submission announced."
        );

        mockMvc.perform(post("/api/v1/outreach/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.outreachId").isNotEmpty())
                .andExpect(jsonPath("$.prospectId").isNotEmpty())
                .andExpect(jsonPath("$.subject").value("AI Act exposure check"))
                .andExpect(jsonPath("$.body").isNotEmpty())
                .andExpect(jsonPath("$.status").value("GENERATED"))
                .andExpect(jsonPath("$.generationPromptVersion").value("v1.0"))
                .andExpect(jsonPath("$.usedSignals", org.hamcrest.Matchers.hasItem("Python")))
                .andExpect(jsonPath("$.usedSignals", org.hamcrest.Matchers.hasItem("linkedin-profile-public")));
    }

    @Test
    void postGenerate_returns400WhenIdentityMissing() throws Exception {
        GenerateOutreachRequest req = new GenerateOutreachRequest(
                null, null, "Lumeon Health", "lumeon.health",
                null, null, List.of(), null);

        mockMvc.perform(post("/api/v1/outreach/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRecent_returnsArray() throws Exception {
        mockMvc.perform(get("/api/v1/outreach/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getProspects_returnsArray() throws Exception {
        mockMvc.perform(get("/api/v1/prospects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void postSendMock_updatesStatus() throws Exception {
        // Generate first
        GenerateOutreachRequest req = new GenerateOutreachRequest(
                "Tomasz Krawczyk", "CTO", "DataNova", "datanova.de",
                "https://linkedin.com/in/tomasz-krawczyk", null,
                List.of("RAG"), "Series A in April.");
        String responseJson = mockMvc.perform(post("/api/v1/outreach/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String outreachId = jsonMapper.readTree(responseJson).get("outreachId").asString();

        mockMvc.perform(post("/api/v1/outreach/" + outreachId + "/send-mock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT_MOCK"));
    }

    private static ChatResponse chatResponse(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }
}
