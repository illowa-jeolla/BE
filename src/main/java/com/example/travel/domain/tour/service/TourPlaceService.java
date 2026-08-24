package com.example.travel.domain.tour.service;

import com.example.travel.domain.tour.client.TourInfoClient;
import com.example.travel.domain.tour.dto.TourPlaceDetailResponse;
import com.example.travel.domain.tour.dto.TourPlaceMapResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TourPlaceService {
    private final TourInfoClient tourInfoClient;

    public TourPlaceService(TourInfoClient tourInfoClient) {
        this.tourInfoClient = tourInfoClient;
    }

    public TourPlaceMapResponse findMapPlaces(String region, int pageNo, int numOfRows) {
        return tourInfoClient.findPlaces(region, pageNo, numOfRows);
    }

    public TourPlaceDetailResponse findPlaceDetail(String contentId) {
        return tourInfoClient.findPlaceDetail(contentId);
    }

    public TourPlaceMapResponse findNearbyPlaces(BigDecimal latitude, BigDecimal longitude,
                                                 int radius, int pageNo, int numOfRows) {
        return tourInfoClient.findPlacesNearby(
                latitude, longitude, radius, pageNo, numOfRows);
    }
}
