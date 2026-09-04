package com.example.travel.domain.ai.service;

import com.example.travel.domain.ai.model.AiMatchRequestContext;
import com.example.travel.domain.ai.repository.AiCandidateSearchRepository.JobMatch;
import com.example.travel.domain.ai.repository.AiCandidateSearchRepository.PlaceMatch;
import com.example.travel.domain.region.entity.Region;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AiMatchPromptService {
    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    public String searchText(AiMatchRequestContext context) {
        return "희망 직무: " + String.join(", ", context.desiredJobs()) + "\n사용자 생각: " + context.thought();
    }

    public String input(AiMatchRequestContext context, Region region,
                        List<JobMatch> jobs, List<PlaceMatch> places) {
        try {
            return MAPPER.writeValueAsString(Map.of(
                    "preferredRegion", Map.of("id", region.getId(), "name", region.getName()),
                    "desiredJobs", context.desiredJobs(), "priorities", context.priorities(),
                    "thought", context.thought(),
                    "jobCandidates", jobs.stream().map(this::job).toList(),
                    "placeCandidates", places.stream().map(this::place).toList()));
        } catch (Exception exception) { throw new IllegalStateException(exception); }
    }

    public String instructions() {
        return "실제 후보만 사용해 거주 지역에 맞는 일자리와 관광지를 추천하라. "
                + "반드시 전달받은 job id와 place id만 반환하고 각각 최대 3개를 선택한다. "
                + "housingScore와 communityScore는 사용자 생각과 지역 특성에 근거해 0~100으로 평가한다. "
                + "추천 이유는 한국어로 간결하게 작성한다.";
    }

    public JsonNode schema() {
        return MAPPER.valueToTree(Map.of(
                "type", "object", "additionalProperties", false,
                "properties", Map.of(
                        "summary", Map.of("type", "string"),
                        "housingScore", Map.of("type", "integer", "minimum", 0, "maximum", 100),
                        "communityScore", Map.of("type", "integer", "minimum", 0, "maximum", 100),
                        "jobs", selectionArray(), "places", selectionArray()),
                "required", List.of("summary", "housingScore", "communityScore", "jobs", "places")));
    }

    private Map<String, Object> selectionArray() {
        return Map.of("type", "array", "maxItems", 3,
                "items", Map.of("type", "object", "additionalProperties", false,
                        "properties", Map.of("id", Map.of("type", "integer"), "reason", Map.of("type", "string")),
                        "required", List.of("id", "reason")));
    }

    private Map<String, Object> job(JobMatch match) {
        var value = match.candidate();
        return Map.of("id", value.getId(), "source", value.getSource(), "externalId", value.getExternalId(),
                "region", text(match.regionName()),
                "title", value.getTitle(), "company", text(value.getCompanyName()), "address", text(value.getAddress()),
                "description", text(value.getJobDescription()), "semanticScore", score(match.similarity()));
    }

    private Map<String, Object> place(PlaceMatch match) {
        var value = match.candidate();
        return Map.of("id", value.getId(), "contentId", value.getExternalId(), "name", value.getName(),
                "category", text(value.getCategory()), "address", text(value.getAddress()),
                "description", text(value.getDescription()), "semanticScore", score(match.similarity()));
    }

    private String text(String value) { return value == null ? "" : value; }
    private int score(double value) { return (int) Math.round(value * 100); }
}
