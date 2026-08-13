package com.example.travel.domain.tour.controller;

import com.example.travel.domain.tour.dto.TourPlaceDetailResponse;
import com.example.travel.domain.tour.dto.TourPlaceMapResponse;
import com.example.travel.domain.tour.service.TourPlaceService;
import com.example.travel.global.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tour")
public class TourPlaceController {
    private final TourPlaceService tourPlaceService;

    public TourPlaceController(TourPlaceService tourPlaceService) {
        this.tourPlaceService = tourPlaceService;
    }

    @GetMapping("/places")
    public ResponseEntity<ApiResponse<TourPlaceMapResponse>> places(
            @RequestParam String region,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "12") int numOfRows) {
        return ResponseEntity.ok(ApiResponse.success(
                tourPlaceService.findMapPlaces(region, pageNo, numOfRows)));
    }

    @GetMapping("/places/{contentId}")
    public ResponseEntity<ApiResponse<TourPlaceDetailResponse>> placeDetail(@PathVariable String contentId) {
        return ResponseEntity.ok(ApiResponse.success(tourPlaceService.findPlaceDetail(contentId)));
    }
}
