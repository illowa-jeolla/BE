package com.example.travel.domain.ai.service;

import com.example.travel.domain.ai.client.OpenAiClient;
import com.example.travel.domain.ai.client.OpenAiEmbeddingClient;
import com.example.travel.domain.ai.dto.response.AiMatchResultResponse;
import com.example.travel.domain.ai.entity.AiJobCandidate;
import com.example.travel.domain.ai.entity.AiTourPlaceCandidate;
import com.example.travel.domain.ai.enums.AiRequestStatus;
import com.example.travel.domain.ai.enums.PriorityType;
import com.example.travel.domain.ai.exception.AiMatchErrorCode;
import com.example.travel.domain.ai.exception.AiMatchException;
import com.example.travel.domain.ai.model.AiMatchRequestContext;
import com.example.travel.domain.ai.repository.AiCandidateSearchRepository;
import com.example.travel.domain.ai.repository.AiCandidateSearchRepository.JobMatch;
import com.example.travel.domain.ai.repository.AiCandidateSearchRepository.PlaceMatch;
import com.example.travel.domain.region.entity.Region;
import com.example.travel.domain.region.repository.RegionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AiMatchProcessor {
    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();
    private final AiMatchRequestCacheService cacheService;
    private final OpenAiEmbeddingClient embeddingClient;
    private final AiCandidateSearchRepository searchRepository;
    private final AiMatchPromptService promptService;
    private final OpenAiClient openAiClient;
    private final RegionRepository regionRepository;
    private final AiMatchPersistenceService persistenceService;

    public AiMatchProcessor(AiMatchRequestCacheService cacheService, OpenAiEmbeddingClient embeddingClient,
                            AiCandidateSearchRepository searchRepository, AiMatchPromptService promptService,
                            OpenAiClient openAiClient, RegionRepository regionRepository,
                            AiMatchPersistenceService persistenceService) {
        this.cacheService = cacheService; this.embeddingClient = embeddingClient;
        this.searchRepository = searchRepository; this.promptService = promptService;
        this.openAiClient = openAiClient; this.regionRepository = regionRepository;
        this.persistenceService = persistenceService;
    }

    public void process(UUID requestId) {
        AiMatchRequestContext context = cacheService.find(requestId)
                .orElseThrow(() -> new AiMatchException(AiMatchErrorCode.REQUEST_NOT_FOUND));
        try {
            Region region = regionRepository.findActiveById(context.preferredRegionId())
                    .orElseThrow(() -> new AiMatchException(AiMatchErrorCode.REGION_NOT_FOUND));
            float[] query = embeddingClient.embedOne(promptService.searchText(context));
            List<JobMatch> jobs = searchRepository.findJobs(region.getId(), query, 20);
            boolean jobsReplaced = jobs.isEmpty();
            if (jobsReplaced) jobs = searchRepository.findJobsAcrossRegions(query, 20);
            List<PlaceMatch> places = searchRepository.findPlaces(region.getId(), query, 20);
            String instructions = promptService.instructions()
                    + " If jobCandidates or placeCandidates is empty, return an empty corresponding array.";
            String raw = openAiClient.generateStructured(instructions,
                    promptService.input(context, region, jobs, places), promptService.schema());
            AiSelection selection = MAPPER.readValue(raw, AiSelection.class);
            AiMatchResultResponse response = response(context, region, jobs, places, selection, jobsReplaced);
            persistenceService.save(requestId, context.userId(), response);
            cacheService.delete(requestId);
        } catch (Exception exception) {
            String code = exception instanceof com.example.travel.global.exception.BusinessException business
                    ? business.getCode() : AiMatchErrorCode.INVALID_AI_RESPONSE.code();
            cacheService.save(context.withStatus(AiRequestStatus.FAILED, code));
        }
    }

    private AiMatchResultResponse response(AiMatchRequestContext context, Region region,
                                           List<JobMatch> jobs, List<PlaceMatch> places,
                                           AiSelection selection, boolean jobsReplaced) {
        Map<Long, JobMatch> jobMap = mapJobs(jobs); Map<Long, PlaceMatch> placeMap = mapPlaces(places);
        Set<Long> selectedJobIds = new HashSet<>();
        List<AiMatchResultResponse.Job> selectedJobs = selection.jobs().stream()
                .filter(value -> selectedJobIds.add(value.id()))
                .filter(value -> jobMap.containsKey(value.id())).map(value -> {
            JobMatch match = jobMap.get(value.id()); AiJobCandidate job = match.candidate();
            AiMatchResultResponse.Region jobRegion = match.regionId() == null
                    ? new AiMatchResultResponse.Region(region.getId(), region.getName())
                    : new AiMatchResultResponse.Region(match.regionId(), match.regionName());
            return new AiMatchResultResponse.Job(job.getSource().name(), job.getExternalId(), job.getTitle(), jobRegion,
                    job.getCompanyName(), job.getAddress(), job.getDeadline(), job.getSourceUrl(),
                    score(match.similarity()), value.reason());
        }).toList();
        Set<Long> selectedPlaceIds = new HashSet<>();
        List<AiMatchResultResponse.Place> selectedPlaces = selection.places().stream()
                .filter(value -> selectedPlaceIds.add(value.id()))
                .filter(value -> placeMap.containsKey(value.id())).map(value -> {
            PlaceMatch match = placeMap.get(value.id()); AiTourPlaceCandidate place = match.candidate();
            return new AiMatchResultResponse.Place(place.getExternalId(), place.getName(), place.getCategory(),
                    place.getAddress(), place.getImageUrl(), score(match.similarity()), value.reason());
        }).toList();
        int jobScore = averageJobs(selectedJobs); int tourismScore = averagePlaces(selectedPlaces);
        int housing = clamp(selection.housingScore()); int community = clamp(selection.communityScore());
        int overall = overall(context.priorities(), jobScore, tourismScore, housing, community);
        var scores = new AiMatchResultResponse.Scores(overall, 100, jobScore, tourismScore, housing, community);
        var regionStatus = success("추천 지역을 찾았습니다.");
        var jobStatus = selectedJobs.isEmpty()
                ? failed("추천 가능한 일자리가 없습니다.")
                : jobsReplaced
                ? replaced("선택한 지역에 추천 가능한 일자리가 없어 다른 지역의 일자리로 대체했습니다.")
                : success("추천 일자리를 찾았습니다.");
        var tourismStatus = selectedPlaces.isEmpty()
                ? failed("해당 지역에 추천 가능한 관광지가 없습니다.")
                : success("추천 관광지를 찾았습니다.");
        var result = new AiMatchResultResponse.Result(1,
                new AiMatchResultResponse.Region(region.getId(), region.getName()), scores,
                selection.summary(), regionStatus, jobStatus, tourismStatus, selectedJobs, selectedPlaces);
        AiRequestStatus status = jobsReplaced && !selectedJobs.isEmpty()
                ? AiRequestStatus.REPLACED : AiRequestStatus.COMPLETED;
        return new AiMatchResultResponse(context.requestId(), status, List.of(result));
    }

    private AiMatchResultResponse.SectionStatus success(String message) {
        return new AiMatchResultResponse.SectionStatus(AiMatchResultResponse.SectionState.SUCCESS, message);
    }

    private AiMatchResultResponse.SectionStatus failed(String message) {
        return new AiMatchResultResponse.SectionStatus(AiMatchResultResponse.SectionState.FAILED, message);
    }

    private AiMatchResultResponse.SectionStatus replaced(String message) {
        return new AiMatchResultResponse.SectionStatus(AiMatchResultResponse.SectionState.REPLACED, message);
    }

    private int overall(List<PriorityType> priorities, int job, int tourism, int housing, int community) {
        int[] weights = {40, 30, 20, 10}; Map<PriorityType, Integer> values = Map.of(
                PriorityType.JOB, job, PriorityType.TOURISM, tourism,
                PriorityType.HOUSING, housing, PriorityType.COMMUNITY, community);
        int total = 0; for (int i = 0; i < priorities.size(); i++) total += values.get(priorities.get(i)) * weights[i];
        return Math.round(total / 100f);
    }

    private int averageJobs(List<AiMatchResultResponse.Job> values) {
        return (int) Math.round(values.stream().mapToInt(AiMatchResultResponse.Job::matchScore).average().orElse(0));
    }
    private int averagePlaces(List<AiMatchResultResponse.Place> values) {
        return (int) Math.round(values.stream().mapToInt(AiMatchResultResponse.Place::matchScore).average().orElse(0));
    }
    private int score(double value) { return clamp((int) Math.round(value * 100)); }
    private int clamp(int value) { return Math.min(100, Math.max(0, value)); }
    private Map<Long, JobMatch> mapJobs(List<JobMatch> values) {
        Map<Long, JobMatch> map = new HashMap<>(); values.forEach(value -> map.put(value.candidate().getId(), value)); return map;
    }
    private Map<Long, PlaceMatch> mapPlaces(List<PlaceMatch> values) {
        Map<Long, PlaceMatch> map = new HashMap<>(); values.forEach(value -> map.put(value.candidate().getId(), value)); return map;
    }
    public record AiSelection(String summary, int housingScore, int communityScore,
                              List<Selection> jobs, List<Selection> places) {
        public AiSelection { jobs = jobs == null ? List.of() : jobs; places = places == null ? List.of() : places; }
    }
    public record Selection(Long id, String reason) {}
}
