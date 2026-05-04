package com.gtmzero;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.ai.embedding.EmbeddingModel;

@SpringBootTest
@ActiveProfiles("dev")
class GtmZeroApplicationTests {

    @MockitoBean
    private EmbeddingModel embeddingModel;

    @Test
    void contextLoads() {
    }
}
