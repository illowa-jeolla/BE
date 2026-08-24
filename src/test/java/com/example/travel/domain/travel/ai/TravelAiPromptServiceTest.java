package com.example.travel.domain.travel.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TravelAiPromptServiceTest {

    private final TravelAiPromptService service = new TravelAiPromptService();

    @Test
    void requiresRecommendedTimeInTwentyFourHourFormat() {
        JsonNode itemProperties = service.responseSchema()
                .path("properties").path("days").path("items")
                .path("properties").path("items").path("items")
                .path("properties");

        assertThat(itemProperties.path("recommendedTime").path("pattern").asText())
                .isEqualTo("^([01]\\d|2[0-3]):[0-5]\\d$");
        assertThat(service.instructions()).contains("24시간제 HH:mm");
        assertThat(service.instructions()).contains("travelTip은 반드시 자연스러운 한국어");
    }

    @Test
    void instructsAiToArrangePlacesUsingRouteBoundaryCoordinates() {
        String instructions = service.instructions();

        assertThat(instructions)
                .contains("firstDayStartLocation", "lodgingLocation", "lastDayEndLocation")
                .contains("불필요한 왕복")
                .contains("실제 도로 거리나 이동시간을 추측하지 않는다");
    }
}
