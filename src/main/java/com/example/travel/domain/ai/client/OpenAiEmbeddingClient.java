package com.example.travel.domain.ai.client;

import com.example.travel.domain.ai.config.OpenAiProperties;
import com.example.travel.domain.ai.exception.OpenAiErrorCode;
import com.example.travel.domain.ai.exception.OpenAiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiEmbeddingClient {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final OpenAiProperties properties;
    private final RestClient restClient;

    public OpenAiEmbeddingClient(OpenAiProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(30));
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public List<float[]> embed(List<String> inputs) {
        if (!properties.isEmbeddingConfigured()) {
            throw new OpenAiException(OpenAiErrorCode.NOT_CONFIGURED);
        }
        if (inputs == null || inputs.isEmpty()
                || inputs.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException("Embedding input must not be blank");
        }
        try {
            String body = restClient.post().uri("/v1/embeddings")
                    .body(Map.of(
                            "model", properties.embeddingModel(),
                            "dimensions", properties.embeddingDimensions(),
                            "encoding_format", "float",
                            "input", inputs))
                    .retrieve().body(String.class);
            JsonNode data = OBJECT_MAPPER.readTree(body).path("data");
            if (!data.isArray() || data.size() != inputs.size()) {
                throw new OpenAiException(OpenAiErrorCode.INVALID_RESPONSE);
            }
            List<float[]> ordered = new ArrayList<>(java.util.Collections.nCopies(inputs.size(), null));
            for (JsonNode item : data) {
                int index = item.path("index").asInt(-1);
                JsonNode vector = item.path("embedding");
                if (index < 0 || index >= ordered.size() || !vector.isArray()
                        || vector.size() != properties.embeddingDimensions()) {
                    throw new OpenAiException(OpenAiErrorCode.INVALID_RESPONSE);
                }
                float[] values = new float[vector.size()];
                for (int i = 0; i < vector.size(); i++) values[i] = (float) vector.get(i).asDouble();
                ordered.set(index, values);
            }
            if (ordered.stream().anyMatch(java.util.Objects::isNull)) {
                throw new OpenAiException(OpenAiErrorCode.INVALID_RESPONSE);
            }
            return List.copyOf(ordered);
        } catch (OpenAiException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new OpenAiException(OpenAiErrorCode.API_ERROR, exception);
        } catch (Exception exception) {
            throw new OpenAiException(OpenAiErrorCode.INVALID_RESPONSE, exception);
        }
    }

    public float[] embedOne(String input) {
        return embed(List.of(input)).get(0);
    }
}
