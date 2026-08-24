package com.example.travel.domain.gathering.service;

import com.example.travel.domain.gathering.repository.projection.GatheringSearchProjection;
import com.example.travel.domain.gathering.dto.item.GatheringSearchItem;
import com.example.travel.domain.gathering.dto.request.GatheringSearchRequest;
import com.example.travel.domain.gathering.dto.response.GatheringSearchResponse;
import com.example.travel.domain.gathering.enums.GatheringStatus;
import com.example.travel.domain.gathering.enums.ParticipantStatus;
import com.example.travel.domain.gathering.exception.GatheringErrorCode;
import com.example.travel.domain.gathering.exception.GatheringException;
import com.example.travel.domain.gathering.repository.GatheringRepository;
import com.example.travel.domain.gathering.service.calculator.GatheringRelevanceCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

@Service
public class GatheringSearchService {
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    private final GatheringRepository gatheringRepository;
    private final GatheringRelevanceCalculator relevanceCalculator;
    private final Clock clock;

    public GatheringSearchService(GatheringRepository gatheringRepository,
                                  GatheringRelevanceCalculator relevanceCalculator,
                                  Clock clock) {
        this.gatheringRepository = gatheringRepository;
        this.relevanceCalculator = relevanceCalculator;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public GatheringSearchResponse search(Long userId, GatheringSearchRequest request) {
        validateDateRange(request);

        int page = request.page() == null ? DEFAULT_PAGE : request.page();
        int size = request.size() == null ? DEFAULT_SIZE : request.size();
        LocalTime requestedTime = optionalTime(request.time());
        String requestedConcept = optionalText(request.concept());
        String requestedMeetingPlace = optionalText(request.meetingPlace());

        OffsetDateTime rangeStart = request.startsOn().atStartOfDay(SERVICE_ZONE).toOffsetDateTime();
        OffsetDateTime rangeEnd = request.endsOn().plusDays(1)
                .atStartOfDay(SERVICE_ZONE).toOffsetDateTime();
        OffsetDateTime now = OffsetDateTime.now(clock);
        OffsetDateTime effectiveStart = rangeStart.isAfter(now) ? rangeStart : now;

        if (!effectiveStart.isBefore(rangeEnd)) {
            return empty(page, size);
        }

        List<GatheringSearchItem> sorted = gatheringRepository.findSearchCandidates(
                        userId, request.region().trim(), effectiveStart, rangeEnd,
                        GatheringStatus.OPEN, ParticipantStatus.JOINED)
                .stream()
                .map(candidate -> toItem(
                        candidate, requestedTime, requestedConcept, requestedMeetingPlace))
                .sorted(comparator(requestedTime != null
                        || requestedConcept != null
                        || requestedMeetingPlace != null))
                .toList();

        long offset = (long) page * size;
        if (offset >= sorted.size()) {
            return new GatheringSearchResponse(List.of(), page, size, sorted.size(), false);
        }
        int fromIndex = (int) offset;
        int toIndex = Math.min(fromIndex + size, sorted.size());
        return new GatheringSearchResponse(sorted.subList(fromIndex, toIndex), page, size,
                sorted.size(), toIndex < sorted.size());
    }

    private GatheringSearchItem toItem(GatheringSearchProjection candidate,
                                       LocalTime requestedTime, String requestedConcept,
                                       String requestedMeetingPlace) {
        Double timeScore = requestedTime == null ? null : relevanceCalculator.timeScore(
                requestedTime,
                candidate.startsAt().atZoneSameInstant(SERVICE_ZONE).toLocalTime());
        Double conceptScore = requestedConcept == null ? null
                : relevanceCalculator.conceptScore(requestedConcept, candidate.concept());
        Double meetingPlaceScore = requestedMeetingPlace == null ? null
                : relevanceCalculator.meetingPlaceScore(
                        requestedMeetingPlace, candidate.meetingPlace());
        double relevanceScore = relevanceCalculator.relevanceScore(
                timeScore, conceptScore, meetingPlaceScore);

        return new GatheringSearchItem(candidate.id(), candidate.title(),
                new GatheringSearchItem.RegionSummary(candidate.regionId(), candidate.regionName()),
                candidate.concept(), candidate.meetingPlace(), candidate.startsAt(),
                candidate.capacity(), candidate.participantCount(), candidate.status(),
                candidate.joinedCount() > 0,
                new GatheringSearchItem.CreatorSummary(
                        candidate.creatorId(), candidate.creatorNickname()),
                timeScore == null ? 0.0 : timeScore,
                conceptScore == null ? 0.0 : conceptScore,
                meetingPlaceScore == null ? 0.0 : meetingPlaceScore,
                relevanceScore);
    }

    private Comparator<GatheringSearchItem> comparator(boolean useRelevance) {
        Comparator<GatheringSearchItem> comparator = Comparator
                .comparing(GatheringSearchItem::startsAt)
                .thenComparing(GatheringSearchItem::id);
        if (!useRelevance) {
            return comparator;
        }
        return Comparator.comparingDouble(GatheringSearchItem::relevanceScore)
                .reversed()
                .thenComparing(comparator);
    }

    private void validateDateRange(GatheringSearchRequest request) {
        if (request.endsOn().isBefore(request.startsOn())) {
            throw new GatheringException(GatheringErrorCode.INVALID_DATE_RANGE);
        }
    }

    private LocalTime optionalTime(String value) {
        return value == null || value.isBlank() ? null : LocalTime.parse(value.trim());
    }

    private String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private GatheringSearchResponse empty(int page, int size) {
        return new GatheringSearchResponse(List.of(), page, size, 0, false);
    }
}
