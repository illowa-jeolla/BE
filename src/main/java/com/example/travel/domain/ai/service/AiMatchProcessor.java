package com.example.travel.domain.ai.service;

import com.example.travel.domain.ai.client.OpenAiClient;
import com.example.travel.domain.ai.client.OpenAiEmbeddingClient;
import com.example.travel.domain.ai.dto.response.AiMatchResultResponse;
import com.example.travel.domain.ai.entity.AiJobCandidate;
import com.example.travel.domain.ai.entity.AiTourPlaceCandidate;
import com.example.travel.domain.ai.enums.AiRequestStatus;
import com.example.travel.domain.ai.exception.AiMatchErrorCode;
import com.example.travel.domain.ai.exception.AiMatchException;
import com.example.travel.domain.ai.model.AiMatchRequestContext;
import com.example.travel.domain.ai.repository.AiCandidateSearchRepository;
import com.example.travel.domain.ai.repository.AiCandidateSearchRepository.JobMatch;
import com.example.travel.domain.ai.repository.AiCandidateSearchRepository.PlaceMatch;
import com.example.travel.domain.ai.repository.AiCandidateSearchRepository.PlaceStats;
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
    private final AiMatchScoreCalculator scoreCalculator;

    public AiMatchProcessor(AiMatchRequestCacheService cacheService, OpenAiEmbeddingClient embeddingClient,
                            AiCandidateSearchRepository searchRepository, AiMatchPromptService promptService,
                            OpenAiClient openAiClient, RegionRepository regionRepository,
                            AiMatchPersistenceService persistenceService,
                            AiMatchScoreCalculator scoreCalculator) {
        this.cacheService = cacheService; this.embeddingClient = embeddingClient;
        this.searchRepository = searchRepository; this.promptService = promptService;
        this.openAiClient = openAiClient; this.regionRepository = regionRepository;
        this.persistenceService = persistenceService;
        this.scoreCalculator = scoreCalculator;
    }

    public void process(UUID requestId) {
        AiMatchRequestContext context = cacheService.find(requestId)
                .orElseThrow(() -> new AiMatchException(AiMatchErrorCode.REQUEST_NOT_FOUND));
        try {
            Region region = regionRepository.findActiveById(context.preferredRegionId())
                    .orElseThrow(() -> new AiMatchException(AiMatchErrorCode.REGION_NOT_FOUND));
            List<float[]> queries = embeddingClient.embed(List.of(
                    promptService.jobSearchText(context),
                    promptService.tourismSearchText(context, region)));
            float[] jobQuery = queries.get(0); float[] tourismQuery = queries.get(1);
            List<JobMatch> jobs = searchRepository.findJobs(region.getId(), jobQuery, 20).stream()
                    .filter(value -> scoreCalculator.isEligible(value.similarity())).toList();
            boolean jobsReplaced = jobs.isEmpty();
            if (jobsReplaced) {
                jobs = searchRepository.findJobsAcrossRegions(jobQuery, 20).stream()
                        .filter(value -> scoreCalculator.isEligible(value.similarity())).toList();
            }
            List<PlaceMatch> places = searchRepository.findPlaces(region.getId(), tourismQuery, 20).stream()
                    .filter(value -> scoreCalculator.isEligible(value.similarity())).toList();
            boolean placesReplaced = places.isEmpty();
            if (placesReplaced) {
                places = searchRepository.findPlacesAcrossNearbyRegions(region.getId(), tourismQuery, 20).stream()
                        .filter(value -> scoreCalculator.isEligible(value.similarity())).toList();
            }
            PlaceStats placeStats = searchRepository.findPlaceStats(region.getId());
            String instructions = promptService.instructions()
                    + " If jobCandidates or placeCandidates is empty, return an empty corresponding array.";
            String raw = openAiClient.generateStructured(instructions,
                    promptService.input(context, region, jobs, places), promptService.schema());
            AiSelection selection = MAPPER.readValue(raw, AiSelection.class);
            AiMatchResultResponse response = response(context, region, jobs, places, placeStats,
                    selection, jobsReplaced, placesReplaced);
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
                                           PlaceStats placeStats, AiSelection selection,
                                           boolean jobsReplaced, boolean placesReplaced) {
        Map<Long, JobMatch> jobMap = mapJobs(jobs); Map<Long, PlaceMatch> placeMap = mapPlaces(places);
        Set<Long> selectedJobIds = new HashSet<>();
        List<AiMatchResultResponse.Job> selectedJobs = selection.jobs().stream()
                .filter(value -> selectedJobIds.add(value.id()))
                .filter(value -> jobMap.containsKey(value.id()))
                .filter(value -> scoreCalculator.isEligible(jobMap.get(value.id()).similarity()))
                .map(value -> {
            JobMatch match = jobMap.get(value.id()); AiJobCandidate job = match.candidate();
            AiMatchResultResponse.Region jobRegion = match.regionId() == null
                    ? new AiMatchResultResponse.Region(region.getId(), region.getName())
                    : new AiMatchResultResponse.Region(match.regionId(), match.regionName());
            return new AiMatchResultResponse.Job(job.getSource().name(), job.getExternalId(), job.getTitle(), jobRegion,
                    job.getCompanyName(), job.getAddress(), job.getDeadline(), job.getSourceUrl(),
                    scoreCalculator.jobScore(match.similarity(), job, context.desiredJobs()), value.reason());
        }).toList();
        if (selectedJobs.isEmpty()) selectedJobs = fallbackJob(context, region, jobs);
        Set<Long> selectedPlaceIds = new HashSet<>();
        List<AiMatchResultResponse.Place> selectedPlaces = selection.places().stream()
                .filter(value -> selectedPlaceIds.add(value.id()))
                .filter(value -> placeMap.containsKey(value.id()))
                .filter(value -> scoreCalculator.isEligible(placeMap.get(value.id()).similarity()))
                .map(value -> {
            PlaceMatch match = placeMap.get(value.id()); AiTourPlaceCandidate place = match.candidate();
            return new AiMatchResultResponse.Place(place.getExternalId(), place.getName(), place.getCategory(),
                    place.getAddress(), place.getImageUrl(), scoreCalculator.semanticScore(match.similarity()),
                    value.reason());
        }).toList();
        if (selectedPlaces.isEmpty()) selectedPlaces = fallbackPlace(places);
        int jobScore = averageJobs(selectedJobs);
        int tourismScore = scoreCalculator.tourismScore(averagePlaces(selectedPlaces),
                placeStats.candidateCount(), placeStats.categoryDiversity());
        int housing = clamp(selection.housingScore()); int community = clamp(selection.communityScore());
        int regionScore = 100;
        int overall = scoreCalculator.overall(context.priorities(), regionScore,
                jobScore, tourismScore, housing, community);
        var scores = new AiMatchResultResponse.Scores(overall, regionScore, jobScore,
                tourismScore, housing, community);
        var regionStatus = success("추천 지역을 찾았습니다.");
        var jobStatus = selectedJobs.isEmpty()
                ? failed("추천 가능한 일자리가 없습니다.")
                : jobsReplaced
                ? replaced("선택한 지역에 추천 가능한 일자리가 없어 다른 지역의 일자리로 대체했습니다.")
                : success("추천 일자리를 찾았습니다.");
        var tourismStatus = selectedPlaces.isEmpty()
                ? failed("해당 지역에 추천 가능한 관광지가 없습니다.")
                : placesReplaced
                ? replaced("선택한 지역에 추천 가능한 관광지가 없어 가까운 지역의 관광지로 대체했습니다.")
                : success("추천 관광지를 찾았습니다.");
        var result = new AiMatchResultResponse.Result(1,
                new AiMatchResultResponse.Region(region.getId(), region.getName()), scores,
                selection.summary(), regionStatus, jobStatus, tourismStatus, selectedJobs, selectedPlaces);
        AiRequestStatus status = (jobsReplaced && !selectedJobs.isEmpty())
                || (placesReplaced && !selectedPlaces.isEmpty())
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

    private int averageJobs(List<AiMatchResultResponse.Job> values) {
        return (int) Math.round(values.stream().mapToInt(AiMatchResultResponse.Job::matchScore).average().orElse(0));
    }
    private int averagePlaces(List<AiMatchResultResponse.Place> values) {
        return (int) Math.round(values.stream().mapToInt(AiMatchResultResponse.Place::matchScore).average().orElse(0));
    }
    private int clamp(int value) { return Math.min(100, Math.max(0, value)); }

    private List<AiMatchResultResponse.Job> fallbackJob(AiMatchRequestContext context, Region region,
                                                        List<JobMatch> jobs) {
        return jobs.stream().filter(value -> scoreCalculator.isEligible(value.similarity()))
                .findFirst().map(match -> {
                    AiJobCandidate job = match.candidate();
                    AiMatchResultResponse.Region jobRegion = match.regionId() == null
                            ? new AiMatchResultResponse.Region(region.getId(), region.getName())
                            : new AiMatchResultResponse.Region(match.regionId(), match.regionName());
                    return List.of(new AiMatchResultResponse.Job(job.getSource().name(), job.getExternalId(),
                            job.getTitle(), jobRegion, job.getCompanyName(), job.getAddress(), job.getDeadline(),
                            job.getSourceUrl(), scoreCalculator.jobScore(match.similarity(), job,
                            context.desiredJobs()), "입력한 조건과 가장 가까운 일자리입니다."));
                }).orElseGet(List::of);
    }

    private List<AiMatchResultResponse.Place> fallbackPlace(List<PlaceMatch> places) {
        return places.stream().filter(value -> scoreCalculator.isEligible(value.similarity()))
                .findFirst().map(match -> {
                    AiTourPlaceCandidate place = match.candidate();
                    return List.of(new AiMatchResultResponse.Place(place.getExternalId(), place.getName(),
                            place.getCategory(), place.getAddress(), place.getImageUrl(),
                            scoreCalculator.semanticScore(match.similarity()),
                            "생활 조건과 가장 가까운 관광지입니다."));
                }).orElseGet(List::of);
    }
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
