package com.example.travel.domain.tour.service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

record TourRegion(String name, BigDecimal longitude, BigDecimal latitude) {
    private static final Map<String, TourRegion> REGIONS = Map.ofEntries(
            Map.entry("전주", new TourRegion("전주", new BigDecimal("127.1480"), new BigDecimal("35.8242"))),
            Map.entry("군산", new TourRegion("군산", new BigDecimal("126.7368"), new BigDecimal("35.9677"))),
            Map.entry("남원", new TourRegion("남원", new BigDecimal("127.3904"), new BigDecimal("35.4164"))),
            Map.entry("목포", new TourRegion("목포", new BigDecimal("126.3922"), new BigDecimal("34.8118"))),
            Map.entry("광주", new TourRegion("광주", new BigDecimal("126.8526"), new BigDecimal("35.1595"))),
            Map.entry("순천", new TourRegion("순천", new BigDecimal("127.4872"), new BigDecimal("34.9506"))),
            Map.entry("여수", new TourRegion("여수", new BigDecimal("127.6622"), new BigDecimal("34.7604"))),
            Map.entry("보성", new TourRegion("보성", new BigDecimal("127.0801"), new BigDecimal("34.7715"))),
            Map.entry("완도", new TourRegion("완도", new BigDecimal("126.7551"), new BigDecimal("34.3110")))
    );

    static Optional<TourRegion> find(String region) {
        if (region == null) return Optional.empty();
        return Optional.ofNullable(REGIONS.get(region.trim()));
    }
}
