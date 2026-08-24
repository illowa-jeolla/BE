package com.example.travel.domain.travel.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ManualTravelDayRequest(
        @Min(1) int dayNumber,
        @NotEmpty List<@Valid ManualTravelPlaceRequest> places
) {}
