package com.example.travel.domain.travel.ai;

import com.example.travel.domain.ai.client.OpenAiClient;
import com.example.travel.domain.ai.exception.OpenAiErrorCode;
import com.example.travel.domain.ai.exception.OpenAiException;
import com.example.travel.domain.travel.ai.dto.AiTravelGuideResult;
import com.example.travel.domain.travel.dto.response.TravelCandidateItem;
import com.example.travel.domain.travel.entity.TravelRecommendationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TravelAiServiceTest {
    @Mock private OpenAiClient openAiClient;
    @Mock private TravelAiPromptService promptService;
    @Mock private TravelRecommendationRequest request;

    private TravelAiService service;

    @BeforeEach
    void setUp() {
        service = new TravelAiService(openAiClient, promptService);
        when(request.getStartsOn()).thenReturn(LocalDate.of(2026, 9, 1));
        when(request.getEndsOn()).thenReturn(LocalDate.of(2026, 9, 1));
        when(request.getDailyPlaceCounts()).thenReturn(new Integer[]{1});
        when(promptService.instructions()).thenReturn("instructions");
        when(promptService.input(any(), any())).thenReturn("input");
        when(promptService.responseSchema()).thenReturn(new ObjectMapper().createObjectNode());
    }

    @Test
    void acceptsOnlyCandidateIds() {
        when(openAiClient.generateStructured(any(), any(), any())).thenReturn(json("100"));

        AiTravelGuideResult result = service.generate(request, List.of(candidate("100")));

        assertThat(result.days().get(0).items().get(0).contentId()).isEqualTo("100");
    }

    @Test
    void rejectsIdOutsideCandidates() {
        when(openAiClient.generateStructured(any(), any(), any())).thenReturn(json("999"));

        assertThatThrownBy(() -> service.generate(request, List.of(candidate("100"))))
                .isInstanceOf(OpenAiException.class)
                .extracting("code")
                .isEqualTo(OpenAiErrorCode.INVALID_RESPONSE.code());
    }

    @Test
    void rejectsNonIsoRecommendedTime() {
        when(openAiClient.generateStructured(any(), any(), any()))
                .thenReturn(json("100").replace("10:00", "오후"));

        assertThatThrownBy(() -> service.generate(request, List.of(candidate("100"))))
                .isInstanceOf(OpenAiException.class)
                .extracting("code")
                .isEqualTo(OpenAiErrorCode.INVALID_RESPONSE.code());
    }

    private String json(String contentId) {
        return """
                {"title":"완도 여행","summary":"요약","days":[{"dayNumber":1,
                "title":"바다 여행","items":[{"contentId":"%s","order":1,
                "recommendedTime":"10:00","stayMinutes":90,"reason":"가깝습니다."}]}],
                "travelTip":"편한 신발을 준비하세요."}
                """.formatted(contentId);
    }

    private TravelCandidateItem candidate(String id) {
        return new TravelCandidateItem(id, "완도타워", "주소", null,
                new BigDecimal("34.31"), new BigDecimal("126.75"), 1000, 80);
    }
}
