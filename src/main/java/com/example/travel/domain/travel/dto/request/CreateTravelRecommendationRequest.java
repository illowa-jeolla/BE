package com.example.travel.domain.travel.dto.request;

import com.example.travel.domain.travel.enums.CompanionType;
import com.example.travel.domain.travel.enums.TransportType;
import com.example.travel.domain.travel.enums.TravelTheme;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;
import java.util.List;

public record CreateTravelRecommendationRequest(
        @NotNull @Positive Long regionId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull @Valid AccommodationRequest accommodation,
        @NotNull @Valid RouteLocationRequest startLocation,
        @NotNull @Valid RouteLocationRequest endLocation,
        @NotEmpty @Size(max = 4) Set<TravelTheme> themes,
        @NotEmpty @Size(max = 7) List<@NotNull @Min(1) @Max(5) Integer> dailyPlaceCounts,
        @NotNull TransportType transportType,
        @NotNull CompanionType companionType
) {
}
