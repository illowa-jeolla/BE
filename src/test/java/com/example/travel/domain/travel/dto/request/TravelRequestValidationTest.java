package com.example.travel.domain.travel.dto.request;

import com.example.travel.domain.travel.enums.CompanionType;
import com.example.travel.domain.travel.enums.TransportType;
import com.example.travel.domain.travel.enums.TravelTheme;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TravelRequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsNullThemeElements() {
        Set<TravelTheme> themes = new HashSet<>();
        themes.add(null);
        CreateTravelRecommendationRequest request = new CreateTravelRecommendationRequest(
                1L, LocalDate.now(), LocalDate.now(), accommodation(), routeLocation(),
                routeLocation(), themes, List.of(1), TransportType.CAR, CompanionType.SOLO);

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString()
                        .startsWith("themes"));
    }

    @Test
    void rejectsNullManualDayAndPlaceElements() {
        List<ManualTravelPlaceRequest> places = new ArrayList<>();
        places.add(null);
        List<ManualTravelDayRequest> days = new ArrayList<>();
        days.add(new ManualTravelDayRequest(1, places));
        days.add(null);
        CreateManualTravelGuideRequest request = new CreateManualTravelGuideRequest(
                1L, LocalDate.now(), LocalDate.now().plusDays(1), accommodation(),
                routeLocation(), routeLocation(), Set.of(TravelTheme.PHOTO), List.of(1, 1),
                TransportType.CAR, CompanionType.SOLO, null, days);

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .anyMatch(path -> path.startsWith("days[0].places"))
                .anyMatch(path -> path.startsWith("days[1]"));
    }

    private AccommodationRequest accommodation() {
        return new AccommodationRequest("lodging", "숙소", "주소",
                new BigDecimal("34.7"), new BigDecimal("127.7"));
    }

    private RouteLocationRequest routeLocation() {
        return new RouteLocationRequest("route", "장소", "주소",
                new BigDecimal("34.7"), new BigDecimal("127.7"));
    }
}
