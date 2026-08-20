package com.example.travel.domain.ai.client;

import com.example.travel.domain.ai.config.OpenAiProperties;
import com.example.travel.domain.ai.exception.OpenAiErrorCode;
import com.example.travel.domain.ai.exception.OpenAiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class OpenAiClient {
    private static final int MAX_ATTEMPTS = 3;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(25);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final OpenAiProperties properties;
    private final Function<Map<String, Object>, String> responseFetcher;

    @Autowired
    public OpenAiClient(OpenAiProperties properties) {
        this(properties, createResponseFetcher(properties));
    }

    OpenAiClient(OpenAiProperties properties,
                 Function<Map<String, Object>, String> responseFetcher) {
        this.properties = properties;
        this.responseFetcher = responseFetcher;
    }

    public String generateStructured(String instructions, String input, JsonNode schema) {
        if (!properties.isConfigured()) {
            throw new OpenAiException(OpenAiErrorCode.NOT_CONFIGURED);
        }

        Map<String, Object> request = request(instructions, input, schema);
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return outputText(responseFetcher.apply(request));
            } catch (RestClientResponseException exception) {
                if (exception.getStatusCode().value() == 429) {
                    if (attempt < MAX_ATTEMPTS) continue;
                    throw new OpenAiException(OpenAiErrorCode.RATE_LIMITED, exception);
                }
                if (exception.getStatusCode().is5xxServerError() && attempt < MAX_ATTEMPTS) {
                    continue;
                }
                throw new OpenAiException(OpenAiErrorCode.API_ERROR, exception);
            } catch (ResourceAccessException exception) {
                if (attempt < MAX_ATTEMPTS) continue;
                throw new OpenAiException(OpenAiErrorCode.TIMEOUT, exception);
            }
        }
        throw new OpenAiException(OpenAiErrorCode.API_ERROR);
    }

    private Map<String, Object> request(String instructions, String input, JsonNode schema) {
        Map<String, Object> format = new LinkedHashMap<>();
        format.put("type", "json_schema");
        format.put("name", "travel_guide");
        format.put("strict", true);
        // RestClient's JSON converter must receive plain JSON-compatible values here.
        // Passing a Jackson JsonNode through Spring Boot 4's converter can serialize
        // the node as a POJO instead of preserving the JSON Schema object itself.
        format.put("schema", OBJECT_MAPPER.convertValue(schema, Object.class));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", properties.model());
        request.put("instructions", instructions);
        request.put("input", input);
        request.put("text", Map.of("format", format));
        request.put("max_output_tokens", 5_000);
        request.put("store", false);
        return request;
    }

    private String outputText(String body) {
        if (body == null || body.isBlank()) {
            throw new OpenAiException(OpenAiErrorCode.INVALID_RESPONSE);
        }
        try {
            JsonNode response = OBJECT_MAPPER.readTree(body);
            for (JsonNode output : response.path("output")) {
                if (!"message".equals(output.path("type").asText())) continue;
                for (JsonNode content : output.path("content")) {
                    if ("output_text".equals(content.path("type").asText())) {
                        String text = content.path("text").asText();
                        if (!text.isBlank()) return text;
                    }
                }
            }
            throw new OpenAiException(OpenAiErrorCode.INVALID_RESPONSE);
        } catch (JsonProcessingException exception) {
            throw new OpenAiException(OpenAiErrorCode.INVALID_RESPONSE, exception);
        }
    }

    private static Function<Map<String, Object>, String> createResponseFetcher(
            OpenAiProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        RestClient client = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        return request -> client.post()
                .uri("/v1/responses")
                .body(request)
                .retrieve()
                .body(String.class);
    }
}
