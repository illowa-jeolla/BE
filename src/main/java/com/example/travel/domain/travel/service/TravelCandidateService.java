package com.example.travel.domain.travel.service;

import com.example.travel.domain.tour.dto.TourPlaceItem;
import com.example.travel.domain.tour.dto.TourPlaceMapResponse;
import com.example.travel.domain.tour.service.TourPlaceService;
import com.example.travel.domain.travel.dto.response.TravelCandidateItem;
import com.example.travel.domain.travel.enums.TransportType;
import com.example.travel.domain.travel.enums.TravelTheme;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class TravelCandidateService {
    private static final int SEARCH_RADIUS_METERS = 20_000;
    private static final int PAGE_SIZE = 30;

    private final TourPlaceService tourPlaceService;

    public TravelCandidateService(TourPlaceService tourPlaceService) {
        this.tourPlaceService = tourPlaceService;
    }

    public List<TravelCandidateItem> findCandidates(BigDecimal latitude, BigDecimal longitude,
                                                     Set<TravelTheme> themes,
                                                     TransportType transportType,
                                                     int requestedPlaces,
                                                     Set<String> excludedContentIds) {
        Map<String, TourPlaceItem> usablePlaces = new LinkedHashMap<>();
        int pageNo = 1;
        while (true) {
            TourPlaceMapResponse response = tourPlaceService.findNearbyPlaces(
                    latitude, longitude, SEARCH_RADIUS_METERS, pageNo, PAGE_SIZE);
            response.items().stream()
                    .filter(this::hasUsableLocation)
                    .filter(place -> !excludedContentIds.contains(place.contentId()))
                    .forEach(place -> usablePlaces.putIfAbsent(place.contentId(), place));

            boolean enoughCandidates = usablePlaces.size() >= requestedPlaces;
            boolean lastPage = response.items().isEmpty()
                    || (long) pageNo * PAGE_SIZE >= response.totalCount();
            if (enoughCandidates || lastPage) break;
            pageNo++;
        }

        return usablePlaces.values().stream()
                .map(place -> toCandidate(place, themes, transportType))
                .sorted(Comparator.comparingInt(TravelCandidateItem::baseScore).reversed()
                        .thenComparing(item -> item.distanceMeters() == null
                                ? Integer.MAX_VALUE : item.distanceMeters()))
                .toList();
    }

    private TravelCandidateItem toCandidate(TourPlaceItem place, Set<TravelTheme> themes,
                                             TransportType transportType) {
        int score = distanceScore(place.distanceMeters())
                + themeScore(place, themes)
                + transportScore(place.distanceMeters(), transportType);
        return new TravelCandidateItem(place.contentId(), place.title(), place.address(),
                place.thumbnailUrl(), place.mapY(), place.mapX(), place.distanceMeters(), score);
    }

    private int distanceScore(Integer distanceMeters) {
        if (distanceMeters == null) return 0;
        return Math.max(0, 60 - (int) Math.round(distanceMeters / 333.0));
    }

    private int themeScore(TourPlaceItem place, Set<TravelTheme> themes) {
        String searchable = String.join(" ", safe(place.title()), safe(place.category1()),
                safe(place.category2()), safe(place.category3())).toLowerCase(Locale.ROOT);
        int matches = 0;
        for (TravelTheme theme : themes) {
            if (matches(theme, searchable)) matches++;
        }
        return Math.min(25, matches * 15);
    }

    private boolean matches(TravelTheme theme, String value) {
        return switch (theme) {
            case NATURE_HEALING -> containsAny(value, "a01", "공원", "숲", "해변", "산", "섬", "수목원", "휴양");
            case LOCAL_FOOD -> containsAny(value, "a05", "시장", "먹거리", "음식", "카페");
            case PHOTO -> containsAny(value, "전망", "해변", "정원", "공원", "벽화", "케이블카", "등대");
            case HISTORY_CULTURE -> containsAny(value, "a02", "유적", "박물관", "전시", "사찰", "성", "문화", "기념관");
        };
    }

    private int transportScore(Integer distanceMeters, TransportType transportType) {
        if (distanceMeters == null) return 0;
        return switch (transportType) {
            case WALK -> distanceMeters <= 3_000 ? 15 : distanceMeters <= 6_000 ? 7 : 0;
            case PUBLIC_TRANSIT -> distanceMeters <= 10_000 ? 15 : 8;
            case CAR -> 15;
        };
    }

    private boolean hasUsableLocation(TourPlaceItem place) {
        return place.contentId() != null && !place.contentId().isBlank()
                && place.title() != null && !place.title().isBlank()
                && place.mapX() != null && place.mapY() != null;
    }

    private boolean containsAny(String value, String... tokens) {
        for (String token : tokens) {
            if (value.contains(token)) return true;
        }
        return false;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
