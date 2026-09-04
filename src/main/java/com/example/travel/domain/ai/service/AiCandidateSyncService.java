package com.example.travel.domain.ai.service;

import com.example.travel.domain.ai.client.OpenAiEmbeddingClient;
import com.example.travel.domain.ai.config.OpenAiProperties;
import com.example.travel.domain.ai.entity.AiJobCandidate;
import com.example.travel.domain.ai.entity.AiTourPlaceCandidate;
import com.example.travel.domain.ai.enums.ExternalCandidateSource;
import com.example.travel.domain.ai.repository.AiJobCandidateRepository;
import com.example.travel.domain.ai.repository.AiTourPlaceCandidateRepository;
import com.example.travel.domain.job.client.JunnamPublicJobApiClient;
import com.example.travel.domain.job.dto.*;
import com.example.travel.domain.job.service.ExternalJobService;
import com.example.travel.domain.region.entity.Region;
import com.example.travel.domain.region.repository.RegionRepository;
import com.example.travel.domain.tour.client.TourInfoClient;
import com.example.travel.domain.tour.dto.TourPlaceItem;
import com.example.travel.domain.tour.dto.TourPlaceMapResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class AiCandidateSyncService {
    private static final Logger log = LoggerFactory.getLogger(AiCandidateSyncService.class);
    private static final int EMBEDDING_BATCH_SIZE = 100;
    private static final int TOUR_RADIUS_METERS = 20_000;
    private static final long JOB_RETENTION_DAYS = 180;
    private static final List<String> CLOSED_JOB_TITLE_KEYWORDS = List.of(
            "채용마감", "모집마감", "접수마감", "채용완료", "공고취소",
            "최종합격자", "서류합격자", "면접대상자", "채용결과");
    private final TourInfoClient tourInfoClient;
    private final JunnamPublicJobApiClient junnamClient;
    private final ExternalJobService externalJobService;
    private final RegionRepository regionRepository;
    private final AiTourPlaceCandidateRepository placeRepository;
    private final AiJobCandidateRepository jobRepository;
    private final OpenAiEmbeddingClient embeddingClient;
    private final OpenAiProperties openAiProperties;
    private final Clock clock;

    public AiCandidateSyncService(TourInfoClient tourInfoClient, JunnamPublicJobApiClient junnamClient,
                                  ExternalJobService externalJobService, RegionRepository regionRepository,
                                  AiTourPlaceCandidateRepository placeRepository,
                                  AiJobCandidateRepository jobRepository,
                                  OpenAiEmbeddingClient embeddingClient,
                                  OpenAiProperties openAiProperties, Clock clock) {
        this.tourInfoClient = tourInfoClient; this.junnamClient = junnamClient;
        this.externalJobService = externalJobService; this.regionRepository = regionRepository;
        this.placeRepository = placeRepository; this.jobRepository = jobRepository;
        this.embeddingClient = embeddingClient; this.openAiProperties = openAiProperties; this.clock = clock;
    }

    public void syncTourPlaces() {
        OffsetDateTime startedAt = OffsetDateTime.now(clock);
        Map<String, AiTourPlaceCandidate> changed = new LinkedHashMap<>();
        List<Region> activeRegions = regionRepository.findAllByActiveTrueOrderByNameAsc();
        Map<String, Region> regions = regionsByName(activeRegions);
        Set<String> observedExternalIds = new HashSet<>();
        boolean collectionComplete = true;
        boolean receivedAnyItems = false;
        int added = 0;
        for (Region region : activeRegions) {
            if (region.getLatitude() == null || region.getLongitude() == null) continue;
            int page = 1; int total;
            do {
                TourPlaceMapResponse response = tourInfoClient.findPlacesNearby(region.getLatitude(),
                        region.getLongitude(), TOUR_RADIUS_METERS, page, 30);
                total = response.totalCount();
                if (response.items().isEmpty()) {
                    collectionComplete = false;
                    break;
                }
                receivedAnyItems = true;
                for (TourPlaceItem item : response.items()) {
                    if (blank(item.contentId()) || blank(item.title())) continue;
                    observedExternalIds.add(item.contentId());
                    Optional<AiTourPlaceCandidate> existing = placeRepository
                            .findBySourceAndExternalId(ExternalCandidateSource.TOUR_INFO, item.contentId());
                    Region candidateRegion = resolvePlaceRegion(item, existing.orElse(null), region,
                            activeRegions, regions);
                    AiTourPlaceCandidate candidate = existing.orElseGet(() -> AiTourPlaceCandidate.create(
                            item.contentId(), candidateRegion, item.title(), category(item), item.address(), null,
                            item.thumbnailUrl(), item.mapY(), item.mapX(), startedAt));
                    if (existing.isEmpty()) added++;
                    candidate.refresh(candidateRegion, item.title(), category(item), item.address(), candidate.getDescription(),
                            item.thumbnailUrl(), item.mapY(), item.mapX(), startedAt);
                    String text = placeText(candidate); String hash = sha256(text);
                    if (candidate.requiresEmbedding(hash, openAiProperties.embeddingModel())) {
                        candidate.invalidateEmbedding();
                        changed.put(item.contentId(), candidate);
                    }
                    placeRepository.save(candidate);
                }
                page++;
            } while ((page - 1) * 30 < total);
        }
        embedPlaces(new ArrayList<>(changed.values()), startedAt);
        List<AiTourPlaceCandidate> deactivated = List.of();
        if (collectionComplete && receivedAnyItems) {
            deactivated = placeRepository
                    .findAllBySourceAndActiveTrueAndLastSeenAtBefore(ExternalCandidateSource.TOUR_INFO, startedAt)
                    .stream().filter(value -> !observedExternalIds.contains(value.getExternalId())).toList();
            deactivated.forEach(value -> { value.deactivate(startedAt); placeRepository.save(value); });
        } else {
            log.warn("AI 관광지 후보 수집이 비어 있거나 완료되지 않아 기존 후보 비활성화를 건너뜁니다.");
        }
        log.info("AI 관광지 후보 비교 결과: {}건 추가했습니다. {}건의 임베딩을 갱신했습니다. {}건 비활성화했습니다.",
                added, Math.max(0, changed.size() - added), deactivated.size());
    }

    public void syncJunnamJobs() {
        OffsetDateTime startedAt = OffsetDateTime.now(clock); List<AiJobCandidate> changed = new ArrayList<>();
        Map<String, Region> regions = regionsByName();
        Set<String> observedExternalIds = new HashSet<>();
        boolean collectionComplete = true;
        boolean receivedAnyItems = false;
        int added = 0;
        int excluded = 0;
        int page = 1; int total;
        do {
            JunnamPublicJobListResponse response = junnamClient.findJobs(page, 100, 100);
            total = response.totalCount();
            if (response.items().isEmpty()) {
                collectionComplete = false;
                break;
            }
            receivedAnyItems = true;
            for (JunnamPublicJobItem item : response.items()) {
                String externalId = first(item.rawFields(), "jobKey", "id", "seq", "nttId");
                if (blank(externalId)) externalId = sha256(text(item.title()) + '|' + text(item.companyName()) + '|' + text(item.address()));
                if (blank(item.title())) continue;
                LocalDate postedAt = date(first(item.rawFields(), "jobInsertDt", "insertDt", "regDt", "createdAt"));
                LocalDate deadline = date(first(item.rawFields(), "jobEndDt", "endDt", "deadline", "rcptDdlnDe"));
                if (!isEligibleJob(postedAt, deadline, item.title(), LocalDate.now(clock))) {
                    excluded++;
                    continue;
                }
                observedExternalIds.add(externalId);
                String resolvedExternalId = externalId;
                Region region = resolveJunnamRegion(item, regions);
                Optional<AiJobCandidate> existing = jobRepository
                        .findBySourceAndExternalId(ExternalCandidateSource.JUNNAM_PUBLIC_JOB, resolvedExternalId);
                AiJobCandidate candidate = existing.orElseGet(() -> AiJobCandidate.create(
                        ExternalCandidateSource.JUNNAM_PUBLIC_JOB, resolvedExternalId, region, item.title(),
                        item.companyName(), item.address(),
                        first(item.rawFields(), "jobContent", "content", "cn", "detail"), null,
                        first(item.rawFields(), "salary", "wageAmt", "pay"), postedAt, deadline,
                        item.homepageUrl(), startedAt));
                if (existing.isEmpty()) added++;
                candidate.refresh(region, item.title(), item.companyName(), item.address(),
                        first(item.rawFields(), "jobContent", "content", "cn", "detail"), null,
                        first(item.rawFields(), "salary", "wageAmt", "pay"), postedAt, deadline,
                        item.homepageUrl(), startedAt);
                collectJobEmbedding(candidate, changed); jobRepository.save(candidate);
            }
            page++;
        } while ((page - 1) * 100 < total);
        embedJobs(changed, startedAt);
        int deactivated = collectionComplete && receivedAnyItems
                ? deactivateJobs(ExternalCandidateSource.JUNNAM_PUBLIC_JOB, startedAt, observedExternalIds) : 0;
        if (!collectionComplete || !receivedAnyItems) {
            log.warn("AI 전남 일자리 후보 수집이 비어 있거나 완료되지 않아 기존 후보 비활성화를 건너뜁니다.");
        }
        log.info("AI 전남 일자리 후보 비교 결과: {}건 추가했습니다. {}건의 임베딩을 갱신했습니다. {}건 제외하고 {}건 비활성화했습니다.",
                added, Math.max(0, changed.size() - added), excluded, deactivated);
    }

    public void syncTourJobs() {
        OffsetDateTime startedAt = OffsetDateTime.now(clock); List<AiJobCandidate> changed = new ArrayList<>();
        Map<String, Region> regions = regionsByName();
        Set<String> observedExternalIds = new HashSet<>();
        boolean collectionComplete = true;
        boolean receivedAnyItems = false;
        int added = 0;
        int excluded = 0;
        int page = 1; int total;
        do {
            TourJobListResponse response = externalJobService.findJeonnamGwangjuTourJobs(
                    new TourJobSearchCondition(page, 100, "D", null,
                    null, null, null, null, null, null, null, null, null, null, null, null));
            total = response.totalCount();
            if (response.items().isEmpty()) {
                collectionComplete = false;
                break;
            }
            receivedAnyItems = true;
            for (TourJobItem item : response.items()) {
                if (blank(item.employmentInfoNo()) || blank(item.title())) continue;
                LocalDate postedAt = date(item.registeredAt());
                LocalDate deadline = date(item.receiptDeadlineDate());
                if (!isEligibleJob(postedAt, deadline, item.title(), LocalDate.now(clock))) {
                    excluded++;
                    continue;
                }
                observedExternalIds.add(item.employmentInfoNo());
                Region region = resolveRegion(item.workplaceAddress(), regions);
                Optional<AiJobCandidate> existing = jobRepository
                        .findBySourceAndExternalId(ExternalCandidateSource.TOUR_JOB, item.employmentInfoNo());
                AiJobCandidate candidate = existing.orElseGet(() -> AiJobCandidate.create(
                        ExternalCandidateSource.TOUR_JOB, item.employmentInfoNo(), region, item.title(),
                        item.companyName(), item.workplaceAddress(), jobCodes(item), item.employmentTypeCode1(),
                        item.wageAmount(), postedAt, deadline, null, startedAt));
                if (existing.isEmpty()) added++;
                candidate.refresh(region, item.title(), item.companyName(), item.workplaceAddress(), jobCodes(item),
                        item.employmentTypeCode1(), item.wageAmount(), postedAt, deadline, null, startedAt);
                collectJobEmbedding(candidate, changed); jobRepository.save(candidate);
            }
            page++;
        } while ((page - 1) * 100 < total);
        embedJobs(changed, startedAt);
        int deactivated = collectionComplete && receivedAnyItems
                ? deactivateJobs(ExternalCandidateSource.TOUR_JOB, startedAt, observedExternalIds) : 0;
        if (!collectionComplete || !receivedAnyItems) {
            log.warn("AI 관광 일자리 후보 수집이 비어 있거나 완료되지 않아 기존 후보 비활성화를 건너뜁니다.");
        }
        log.info("AI 전남·광주 관광 일자리 후보 비교 결과: {}건 추가했습니다. {}건의 임베딩을 갱신했습니다. {}건 제외하고 {}건 비활성화했습니다.",
                added, Math.max(0, changed.size() - added), excluded, deactivated);
    }

    @Transactional
    public void cleanupInactive() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        long deletedJobs = jobRepository.deleteByActiveFalseAndInactiveAtBefore(now.minusDays(30));
        long deletedPlaces = placeRepository.deleteByActiveFalseAndInactiveAtBefore(now.minusDays(90));
        log.info("AI 비활성 후보 정리 결과: 일자리 {}건, 관광지 {}건 삭제했습니다.", deletedJobs, deletedPlaces);
    }

    private void collectJobEmbedding(AiJobCandidate candidate, List<AiJobCandidate> changed) {
        String text = jobText(candidate); String hash = sha256(text);
        if (candidate.requiresEmbedding(hash, openAiProperties.embeddingModel())) {
            candidate.invalidateEmbedding();
            changed.add(candidate);
        }
    }
    private void embedJobs(List<AiJobCandidate> values, OffsetDateTime now) {
        for (int from = 0; from < values.size(); from += EMBEDDING_BATCH_SIZE) {
            List<AiJobCandidate> batch = values.subList(from, Math.min(from + EMBEDDING_BATCH_SIZE, values.size()));
            List<String> texts = batch.stream().map(this::jobText).toList(); List<float[]> vectors = embeddingClient.embed(texts);
            for (int i = 0; i < batch.size(); i++) batch.get(i).updateEmbedding(vectors.get(i),
                    openAiProperties.embeddingModel(), sha256(texts.get(i)), now);
            jobRepository.saveAll(batch);
        }
    }
    private void embedPlaces(List<AiTourPlaceCandidate> values, OffsetDateTime now) {
        for (int from = 0; from < values.size(); from += EMBEDDING_BATCH_SIZE) {
            List<AiTourPlaceCandidate> batch = values.subList(from, Math.min(from + EMBEDDING_BATCH_SIZE, values.size()));
            List<String> texts = batch.stream().map(this::placeText).toList(); List<float[]> vectors = embeddingClient.embed(texts);
            for (int i = 0; i < batch.size(); i++) batch.get(i).updateEmbedding(vectors.get(i),
                    openAiProperties.embeddingModel(), sha256(texts.get(i)), now);
            placeRepository.saveAll(batch);
        }
    }
    private int deactivateJobs(ExternalCandidateSource source, OffsetDateTime startedAt,
                               Set<String> observedExternalIds) {
        List<AiJobCandidate> values = jobRepository
                .findAllBySourceAndActiveTrueAndLastSeenAtBefore(source, startedAt).stream()
                .filter(value -> !observedExternalIds.contains(value.getExternalId())).toList();
        values.forEach(value -> { value.deactivate(startedAt); jobRepository.save(value); });
        return values.size();
    }
    private Region resolveJunnamRegion(JunnamPublicJobItem item, Map<String, Region> regions) {
        Region categoryRegion = resolveRegion(item.rawFields().get("jobCategoryNm"), regions);
        if (categoryRegion != null) return categoryRegion;
        Region addressRegion = resolveRegion(item.address(), regions);
        return addressRegion != null ? addressRegion : resolveRegion(item.title(), regions);
    }

    private Map<String, Region> regionsByName() {
        return regionsByName(regionRepository.findAllByActiveTrueOrderByNameAsc());
    }

    private Map<String, Region> regionsByName(List<Region> activeRegions) {
        Map<String, Region> regions = new LinkedHashMap<>();
        activeRegions.forEach(region -> regions.put(normalizeRegionName(region.getName()), region));
        return regions;
    }

    private Region resolvePlaceRegion(TourPlaceItem item, AiTourPlaceCandidate existing, Region searchRegion,
                                      List<Region> activeRegions, Map<String, Region> regions) {
        Region addressRegion = resolveRegion(item.address(), regions);
        if (addressRegion != null) return addressRegion;
        if (existing != null && existing.getRegion() != null) return existing.getRegion();
        Region coordinateRegion = nearestRegion(item.mapY(), item.mapX(), activeRegions);
        return coordinateRegion != null ? coordinateRegion : searchRegion;
    }

    private Region nearestRegion(java.math.BigDecimal latitude, java.math.BigDecimal longitude,
                                 List<Region> regions) {
        if (latitude == null || longitude == null) return null;
        double lat = latitude.doubleValue();
        double lon = longitude.doubleValue();
        return regions.stream()
                .filter(region -> region.getLatitude() != null && region.getLongitude() != null)
                .min(Comparator.comparingDouble(region -> {
                    double latDiff = lat - region.getLatitude().doubleValue();
                    double lonDiff = lon - region.getLongitude().doubleValue();
                    return latDiff * latDiff + lonDiff * lonDiff;
                }))
                .orElse(null);
    }

    private Region resolveRegion(String value, Map<String, Region> regions) {
        if (blank(value)) return null;
        String normalized = normalizeRegionName(value);
        Region exact = regions.get(normalized);
        if (exact != null) return exact;
        String compact = value.replaceAll("\\s+", "");
        return regions.entrySet().stream()
                .filter(entry -> compact.contains(entry.getKey() + "시")
                        || compact.contains(entry.getKey() + "군")
                        || compact.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private String normalizeRegionName(String value) {
        String normalized = value == null ? "" : value.trim().replaceAll("\\s+", "")
                .replace("전라남도", "").replace("전남", "").replace("광주광역시", "광주");
        if (normalized.endsWith("시") || normalized.endsWith("군") || normalized.endsWith("구")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
    private String placeText(AiTourPlaceCandidate value) {
        return "종류: 관광지\n이름: " + value.getName() + "\n지역: " + regionName(value.getRegion())
                + "\n분류: " + text(value.getCategory()) + "\n주소: " + text(value.getAddress())
                + "\n설명: " + text(value.getDescription());
    }
    private String jobText(AiJobCandidate value) {
        return "종류: 일자리\n공고명: " + value.getTitle() + "\n회사: " + text(value.getCompanyName())
                + "\n지역: " + regionName(value.getRegion()) + "\n주소: " + text(value.getAddress())
                + "\n직무: " + text(value.getJobDescription()) + "\n근무형태: " + text(value.getEmploymentType());
    }
    private String regionName(Region value) { return value == null ? "" : value.getName(); }
    private String category(TourPlaceItem item) { return String.join("/", nonBlank(item.category1(), item.category2(), item.category3())); }
    private List<String> nonBlank(String... values) { return Arrays.stream(values).filter(value -> !blank(value)).toList(); }
    private String jobCodes(TourJobItem item) { return String.join("/", nonBlank(item.upperRecruitJobCode(), item.middleRecruitJobCode(), item.lowerRecruitJobCode())); }
    private String first(Map<String, String> values, String... keys) {
        if (values == null) return null; for (String key : keys) if (!blank(values.get(key))) return values.get(key); return null;
    }
    static boolean isEligibleJob(LocalDate postedAt, LocalDate deadline, String title, LocalDate today) {
        if (title != null && CLOSED_JOB_TITLE_KEYWORDS.stream().anyMatch(title::contains)) return false;
        if (deadline != null) return !deadline.isBefore(today);
        return postedAt != null && !postedAt.isBefore(today.minusDays(JOB_RETENTION_DAYS));
    }

    static LocalDate date(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        if (normalized.length() >= 10 && (normalized.charAt(4) == '-' || normalized.charAt(4) == '.')) {
            normalized = normalized.substring(0, 10);
        }
        for (String pattern : List.of("yyyyMMdd", "yyyy-MM-dd", "yyyy.MM.dd")) {
            try { return LocalDate.parse(normalized, DateTimeFormatter.ofPattern(pattern)); } catch (DateTimeParseException ignored) {}
        } return null;
    }
    private String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception exception) { throw new IllegalStateException(exception); }
    }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String text(String value) { return value == null ? "" : value; }
}
