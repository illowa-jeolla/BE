package com.example.travel.domain.travel.service;

import com.example.travel.domain.tour.dto.TourPlaceItem;
import com.example.travel.domain.tour.dto.TourPlaceMapResponse;
import com.example.travel.domain.tour.service.TourPlaceService;
import com.example.travel.domain.travel.dto.response.ManualTravelPlaceItem;
import com.example.travel.domain.travel.dto.response.ManualTravelPlacePageResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ManualTravelPlaceService {
    private static final int SEARCH_RADIUS_METERS = 20_000;
    private static final int PAGE_SIZE = 30;

    private final TourPlaceService tourPlaceService;

    public ManualTravelPlaceService(TourPlaceService tourPlaceService) {
        this.tourPlaceService = tourPlaceService;
    }

    public ManualTravelPlacePageResponse findPlaces(BigDecimal latitude, BigDecimal longitude,
                                                     int pageNo) {
        TourPlaceMapResponse response = tourPlaceService.findNearbyPlaces(
                latitude, longitude, SEARCH_RADIUS_METERS, pageNo, PAGE_SIZE);
        int totalPages = calculateTotalPages(response.totalCount(), response.numOfRows());
        List<ManualTravelPlaceItem> items = response.items().stream()
                .filter(this::hasUsableLocation)
                .map(this::toItem)
                .toList();

        return new ManualTravelPlacePageResponse(
                response.pageNo(), response.numOfRows(), response.totalCount(), totalPages,
                response.pageNo() > 1,
                response.pageNo() < totalPages,
                items);
    }

    private ManualTravelPlaceItem toItem(TourPlaceItem place) {
        return new ManualTravelPlaceItem(
                place.contentId(), place.title(), place.address(), place.thumbnailUrl(),
                place.mapY(), place.mapX(), place.distanceMeters());
    }

    private boolean hasUsableLocation(TourPlaceItem place) {
        return place.contentId() != null && !place.contentId().isBlank()
                && place.title() != null && !place.title().isBlank()
                && place.mapX() != null && place.mapY() != null;
    }

    private int calculateTotalPages(int totalCount, int pageSize) {
        if (totalCount <= 0 || pageSize <= 0) return 0;
        return (totalCount + pageSize - 1) / pageSize;
    }
}
