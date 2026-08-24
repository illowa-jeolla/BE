package com.example.travel.domain.travel.ai;

import com.example.travel.domain.ai.client.OpenAiClient;
import com.example.travel.domain.ai.exception.OpenAiErrorCode;
import com.example.travel.domain.ai.exception.OpenAiException;
import com.example.travel.domain.travel.ai.dto.AiTravelGuideResult;
import com.example.travel.domain.travel.dto.response.TravelCandidateItem;
import com.example.travel.domain.travel.entity.TravelRecommendationRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TravelAiService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final OpenAiClient openAiClient;
    private final TravelAiPromptService promptService;

    public TravelAiService(OpenAiClient openAiClient, TravelAiPromptService promptService) {
        this.openAiClient = openAiClient;
        this.promptService = promptService;
    }

    public AiTravelGuideResult generate(TravelRecommendationRequest request,
                                        List<TravelCandidateItem> candidates) {
        String raw = openAiClient.generateStructured(promptService.instructions(),
                promptService.input(request, candidates), promptService.responseSchema());
        try {
            AiTravelGuideResult result = OBJECT_MAPPER.readValue(raw, AiTravelGuideResult.class);
            validate(result, request, candidates);
            return result;
        } catch (JsonProcessingException exception) {
            throw new OpenAiException(OpenAiErrorCode.INVALID_RESPONSE, exception);
        }
    }

    private void validate(AiTravelGuideResult result, TravelRecommendationRequest request,
                          List<TravelCandidateItem> candidates) {
        requireText(result.title());
        requireText(result.summary());
        requireText(result.travelTip());
        if (result.days() == null) throw invalidResponse();

        int tripDays = (int) ChronoUnit.DAYS.between(
                request.getStartsOn(), request.getEndsOn()) + 1;
        Integer[] dailyPlaceCounts = request.getDailyPlaceCounts();
        if (dailyPlaceCounts == null || dailyPlaceCounts.length != tripDays
                || result.days().size() != tripDays) throw invalidResponse();

        Set<String> candidateIds = candidates.stream()
                .map(TravelCandidateItem::contentId).collect(java.util.stream.Collectors.toSet());
        Set<String> selectedIds = new HashSet<>();
        Set<Integer> dayNumbers = new HashSet<>();
        for (AiTravelGuideResult.Day day : result.days()) {
            requireText(day.title());
            if (day.dayNumber() < 1 || day.dayNumber() > tripDays
                    || !dayNumbers.add(day.dayNumber()) || day.items() == null
                    || day.items().size() != dailyPlaceCounts[day.dayNumber() - 1]) {
                throw invalidResponse();
            }
            for (int index = 0; index < day.items().size(); index++) {
                AiTravelGuideResult.Item item = day.items().get(index);
                requireText(item.reason());
                if (item.order() != index + 1 || !candidateIds.contains(item.contentId())
                        || !selectedIds.add(item.contentId())
                        || item.stayMinutes() < 30 || item.stayMinutes() > 240) {
                    throw invalidResponse();
                }
                try {
                    LocalTime.parse(item.recommendedTime());
                } catch (DateTimeParseException exception) {
                    throw new OpenAiException(OpenAiErrorCode.INVALID_RESPONSE, exception);
                }
            }
        }
    }

    private void requireText(String value) {
        if (value == null || value.isBlank()) throw invalidResponse();
    }

    private OpenAiException invalidResponse() {
        return new OpenAiException(OpenAiErrorCode.INVALID_RESPONSE);
    }

}
