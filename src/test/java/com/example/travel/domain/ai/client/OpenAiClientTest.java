package com.example.travel.domain.ai.client;

import com.example.travel.domain.ai.config.OpenAiProperties;
import com.example.travel.domain.ai.exception.OpenAiErrorCode;
import com.example.travel.domain.ai.exception.OpenAiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiClientTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void sendsStrictJsonSchemaAndExtractsOutputText() throws Exception {
        AtomicReference<Map<String, Object>> captured = new AtomicReference<>();
        OpenAiClient client = new OpenAiClient(properties(), request -> {
            captured.set(request);
            return """
                    {"output":[{"type":"message","content":[
                      {"type":"output_text","text":"{\\"title\\":\\"완도 여행\\"}"}
                    ]}]}
                    """;
        });

        String result = client.generateStructured("instructions", "input",
                OBJECT_MAPPER.readTree("{\"type\":\"object\"}"));

        assertThat(result).isEqualTo("{\"title\":\"완도 여행\"}");
        assertThat(captured.get()).containsEntry("model", "gpt-test");
        @SuppressWarnings("unchecked")
        Map<String, Object> text = (Map<String, Object>) captured.get().get("text");
        @SuppressWarnings("unchecked")
        Map<String, Object> format = (Map<String, Object>) text.get("format");
        assertThat(format).containsEntry("type", "json_schema")
                .containsEntry("strict", true);
        assertThat(format).containsEntry("name", "travel_guide");
        assertThat(format.get("schema")).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> schema = (Map<String, Object>) format.get("schema");
        assertThat(schema).containsEntry("type", "object");
    }

    @Test
    void rejectsMissingApiConfigurationWithoutCallingTransport() {
        OpenAiProperties properties = new OpenAiProperties("", "gpt-test", "https://example.com");
        OpenAiClient client = new OpenAiClient(properties, request -> {
            throw new AssertionError("transport must not be called");
        });

        assertThatThrownBy(() -> client.generateStructured("i", "d",
                OBJECT_MAPPER.createObjectNode()))
                .isInstanceOf(OpenAiException.class)
                .extracting("code")
                .isEqualTo(OpenAiErrorCode.NOT_CONFIGURED.code());
    }

    @Test
    void rejectsResponseWithoutOutputText() {
        OpenAiClient client = new OpenAiClient(properties(), request -> "{\"output\":[]}");

        assertThatThrownBy(() -> client.generateStructured("i", "d",
                OBJECT_MAPPER.createObjectNode()))
                .isInstanceOf(OpenAiException.class)
                .extracting("code")
                .isEqualTo(OpenAiErrorCode.INVALID_RESPONSE.code());
    }

    private OpenAiProperties properties() {
        return new OpenAiProperties("test-key", "gpt-test", "https://example.com");
    }
}
