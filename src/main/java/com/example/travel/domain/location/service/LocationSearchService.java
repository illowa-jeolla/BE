package com.example.travel.domain.location.service;

import com.example.travel.domain.location.client.KakaoLocalClient;
import com.example.travel.domain.location.dto.LocationSearchResponse;
import com.example.travel.domain.location.dto.LocationSearchItem;
import com.example.travel.domain.location.exception.LocationErrorCode;
import com.example.travel.domain.location.exception.LocationException;
import com.example.travel.domain.region.entity.Region;
import com.example.travel.domain.region.repository.RegionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocationSearchService {
    private static final int REGION_SEARCH_RADIUS_METERS = 20_000;
    private static final int KAKAO_MAX_RESULT_SIZE = 15;

    private final KakaoLocalClient kakaoLocalClient;
    private final RegionRepository regionRepository;

    public LocationSearchService(KakaoLocalClient kakaoLocalClient,
                                 RegionRepository regionRepository) {
        this.kakaoLocalClient = kakaoLocalClient;
        this.regionRepository = regionRepository;
    }

    public LocationSearchResponse search(Long regionId, String query, int size) {
        return search(regionId, query, size, true);
    }

    public LocationSearchResponse searchRoutePoints(Long regionId, String query, int size) {
        return search(regionId, query, size, false);
    }

    private LocationSearchResponse search(Long regionId, String query, int size,
                                          boolean accommodationOnly) {
        Region region = regionRepository.findActiveById(regionId)
                .orElseThrow(() -> new LocationException(LocationErrorCode.REGION_NOT_FOUND));
        if (region.getLatitude() == null || region.getLongitude() == null) {
            throw new LocationException(LocationErrorCode.REGION_COORDINATES_MISSING);
        }

        LocationSearchResponse response = accommodationOnly
                ? kakaoLocalClient.search(query, region.getLatitude(), region.getLongitude(),
                        REGION_SEARCH_RADIUS_METERS, KAKAO_MAX_RESULT_SIZE)
                : kakaoLocalClient.searchRoutePoints(query, region.getLatitude(),
                        region.getLongitude(), REGION_SEARCH_RADIUS_METERS,
                        KAKAO_MAX_RESULT_SIZE);
        List<LocationSearchItem> scopedItems = response.items().stream()
                .filter(item -> belongsToRegion(item, region.getName()))
                .limit(size)
                .toList();
        return new LocationSearchResponse(scopedItems);
    }

    private boolean belongsToRegion(LocationSearchItem item, String regionName) {
        String address = item.roadAddress() == null || item.roadAddress().isBlank()
                ? item.address() : item.roadAddress();
        return address != null && normalize(address).contains(normalizeRegionName(regionName));
    }

    private String normalizeRegionName(String regionName) {
        String normalized = normalize(regionName);
        if (normalized.length() > 1 && (normalized.endsWith("시")
                || normalized.endsWith("군") || normalized.endsWith("구"))) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "").trim();
    }
}
