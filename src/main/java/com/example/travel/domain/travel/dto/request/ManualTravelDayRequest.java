package com.example.travel.domain.travel.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ManualTravelDayRequest(
        @Min(1) int dayNumber,
        @NotEmpty List<@NotNull @Valid ManualTravelPlaceRequest> places
) {}
