package com.example.travel.domain.travel.entity;

import com.example.travel.domain.travel.enums.CompanionType;
import com.example.travel.domain.travel.enums.RecommendationStatus;
import com.example.travel.domain.travel.enums.TransportType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TravelRecommendationRequest {
    private Long id;
    private Long userId;
    private Long regionId;
    private String regionName;
    private String lodgingKakaoPlaceId;
    private String lodgingName;
    private String lodgingAddress;
    private BigDecimal lodgingLatitude;
    private BigDecimal lodgingLongitude;
    private String startPlaceId;
    private String startPlaceName;
    private String startPlaceAddress;
    private BigDecimal startLatitude;
    private BigDecimal startLongitude;
    private String endPlaceId;
    private String endPlaceName;
    private String endPlaceAddress;
    private BigDecimal endLatitude;
    private BigDecimal endLongitude;
    private LocalDate startsOn;
    private LocalDate endsOn;
    private String[] themes;
    private Integer[] dailyPlaceCounts;
    private TransportType transportType;
    private CompanionType companionType;
    private RecommendationStatus status;
    private boolean refreshRequest;

    private TravelRecommendationRequest(Long id, Long userId, Long regionId, String regionName,
            String lodgingKakaoPlaceId, String lodgingName, String lodgingAddress,
            BigDecimal lodgingLatitude, BigDecimal lodgingLongitude, String startPlaceId,
            String startPlaceName, String startPlaceAddress, BigDecimal startLatitude,
            BigDecimal startLongitude, String endPlaceId, String endPlaceName,
            String endPlaceAddress, BigDecimal endLatitude, BigDecimal endLongitude,
            LocalDate startsOn, LocalDate endsOn, String[] themes, Integer[] dailyPlaceCounts,
            TransportType transportType, CompanionType companionType, boolean refreshRequest) {
        this.id = id; this.userId = userId; this.regionId = regionId; this.regionName = regionName;
        this.lodgingKakaoPlaceId = lodgingKakaoPlaceId; this.lodgingName = lodgingName;
        this.lodgingAddress = lodgingAddress; this.lodgingLatitude = lodgingLatitude;
        this.lodgingLongitude = lodgingLongitude; this.startPlaceId = startPlaceId;
        this.startPlaceName = startPlaceName; this.startPlaceAddress = startPlaceAddress;
        this.startLatitude = startLatitude; this.startLongitude = startLongitude;
        this.endPlaceId = endPlaceId; this.endPlaceName = endPlaceName;
        this.endPlaceAddress = endPlaceAddress; this.endLatitude = endLatitude;
        this.endLongitude = endLongitude; this.startsOn = startsOn; this.endsOn = endsOn;
        this.themes = themes.clone(); this.dailyPlaceCounts = dailyPlaceCounts.clone();
        this.transportType = transportType; this.companionType = companionType;
        this.status = RecommendationStatus.PENDING; this.refreshRequest = refreshRequest;
    }

    public static TravelRecommendationRequest create(Long id, Long userId, Long regionId,
            String regionName, String lodgingKakaoPlaceId, String lodgingName,
            String lodgingAddress, BigDecimal lodgingLatitude, BigDecimal lodgingLongitude,
            String startPlaceId, String startPlaceName, String startPlaceAddress,
            BigDecimal startLatitude, BigDecimal startLongitude, String endPlaceId,
            String endPlaceName, String endPlaceAddress, BigDecimal endLatitude,
            BigDecimal endLongitude, LocalDate startsOn, LocalDate endsOn, String[] themes,
            Integer[] dailyPlaceCounts, TransportType transportType, CompanionType companionType) {
        return new TravelRecommendationRequest(id, userId, regionId, regionName,
                lodgingKakaoPlaceId, lodgingName, lodgingAddress, lodgingLatitude,
                lodgingLongitude, startPlaceId, startPlaceName, startPlaceAddress,
                startLatitude, startLongitude, endPlaceId, endPlaceName, endPlaceAddress,
                endLatitude, endLongitude, startsOn, endsOn, themes, dailyPlaceCounts,
                transportType, companionType, false);
    }

    public TravelRecommendationRequest createRefreshRequest(Long newId) {
        return new TravelRecommendationRequest(newId, userId, regionId, regionName,
                lodgingKakaoPlaceId, lodgingName, lodgingAddress, lodgingLatitude,
                lodgingLongitude, startPlaceId, startPlaceName, startPlaceAddress,
                startLatitude, startLongitude, endPlaceId, endPlaceName, endPlaceAddress,
                endLatitude, endLongitude, startsOn, endsOn, themes, dailyPlaceCounts,
                transportType, companionType, true);
    }

    public String[] getThemes() { return themes.clone(); }
    public Integer[] getDailyPlaceCounts() { return dailyPlaceCounts.clone(); }
    public void markProcessing() { status = RecommendationStatus.PROCESSING; }
    public void markCompleted() { status = RecommendationStatus.COMPLETED; }
    public void markFailed() { status = RecommendationStatus.FAILED; }
}
