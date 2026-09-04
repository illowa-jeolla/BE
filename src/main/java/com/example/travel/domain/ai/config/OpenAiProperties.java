package com.example.travel.domain.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openai")
public record OpenAiProperties(String apiKey, String model, String baseUrl,
                               String embeddingModel, Integer embeddingDimensions) {
    public static final int STORAGE_EMBEDDING_DIMENSIONS = 1536;

    public OpenAiProperties {
        if (embeddingDimensions != null && embeddingDimensions != STORAGE_EMBEDDING_DIMENSIONS) {
            throw new IllegalArgumentException("openai.embedding-dimensions must be 1536 to match vector storage");
        }
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank()
                && model != null && !model.isBlank()
                && baseUrl != null && !baseUrl.isBlank();
    }

    public boolean isEmbeddingConfigured() {
        return apiKey != null && !apiKey.isBlank()
                && baseUrl != null && !baseUrl.isBlank()
                && embeddingModel != null && !embeddingModel.isBlank()
                && embeddingDimensions != null && embeddingDimensions == STORAGE_EMBEDDING_DIMENSIONS;
    }
}
