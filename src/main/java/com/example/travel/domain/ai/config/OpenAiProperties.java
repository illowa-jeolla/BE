package com.example.travel.domain.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openai")
public record OpenAiProperties(String apiKey, String model, String baseUrl,
                               String embeddingModel, Integer embeddingDimensions) {
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank()
                && model != null && !model.isBlank()
                && baseUrl != null && !baseUrl.isBlank();
    }

    public boolean isEmbeddingConfigured() {
        return apiKey != null && !apiKey.isBlank()
                && baseUrl != null && !baseUrl.isBlank()
                && embeddingModel != null && !embeddingModel.isBlank()
                && embeddingDimensions != null && embeddingDimensions > 0;
    }
}
