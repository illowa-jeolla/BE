package com.example.travel.domain.ai.service;

import com.example.travel.domain.ai.client.OpenAiEmbeddingClient;
import com.example.travel.domain.ai.config.OpenAiProperties;
import com.example.travel.domain.ai.entity.AiTourPlaceCandidate;
import com.example.travel.domain.ai.enums.ExternalCandidateSource;
import com.example.travel.domain.ai.repository.AiJobCandidateRepository;
import com.example.travel.domain.ai.repository.AiTourPlaceCandidateRepository;
import com.example.travel.domain.job.client.JunnamPublicJobApiClient;
import com.example.travel.domain.job.service.ExternalJobService;
import com.example.travel.domain.region.entity.Region;
import com.example.travel.domain.region.repository.RegionRepository;
import com.example.travel.domain.tour.client.TourInfoClient;
import com.example.travel.domain.tour.dto.TourPlaceItem;
import com.example.travel.domain.tour.dto.TourPlaceMapResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

class AiCandidateSyncServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 3);

    @Test
    void acceptsRecentJobWithoutDeadline() {
        assertThat(AiCandidateSyncService.isEligibleJob(
                TODAY.minusDays(180), null, "관광 기획자 채용", TODAY)).isTrue();
    }

    @Test
    void rejectsOldJobWithoutDeadline() {
        assertThat(AiCandidateSyncService.isEligibleJob(
                TODAY.minusDays(181), null, "관광 기획자 채용", TODAY)).isFalse();
    }

    @Test
    void acceptsOldJobWhenDeadlineHasNotPassed() {
        assertThat(AiCandidateSyncService.isEligibleJob(
                TODAY.minusYears(1), TODAY.plusDays(1), "관광 기획자 채용", TODAY)).isTrue();
    }

    @Test
    void rejectsExpiredOrClosedJob() {
        assertThat(AiCandidateSyncService.isEligibleJob(
                TODAY, TODAY.minusDays(1), "관광 기획자 채용", TODAY)).isFalse();
        assertThat(AiCandidateSyncService.isEligibleJob(
                TODAY, TODAY.plusDays(1), "관광 기획자 채용마감", TODAY)).isFalse();
    }

    @Test
    void parsesSupportedExternalDateFormats() {
        assertThat(AiCandidateSyncService.date("20260903")).isEqualTo(TODAY);
        assertThat(AiCandidateSyncService.date("2026-09-03")).isEqualTo(TODAY);
        assertThat(AiCandidateSyncService.date("2026.09.03")).isEqualTo(TODAY);
        assertThat(AiCandidateSyncService.date("2026-09-03 13:20:10")).isEqualTo(TODAY);
    }

    @Test
    void duplicateContentIdKeepsAdministrativeRegionInsteadOfLastSearchRegion() {
        TourInfoClient tourClient = mock(TourInfoClient.class);
        RegionRepository regionRepository = mock(RegionRepository.class);
        AiTourPlaceCandidateRepository placeRepository = mock(AiTourPlaceCandidateRepository.class);
        OpenAiEmbeddingClient embeddingClient = mock(OpenAiEmbeddingClient.class);
        Region yeosu = Region.createSupportedCity("여수", new BigDecimal("34.7604"), new BigDecimal("127.6622"));
        Region suncheon = Region.createSupportedCity("순천", new BigDecimal("34.9506"), new BigDecimal("127.4872"));
        TourPlaceItem yeosuResult = place("100", "전라남도 여수시 돌산읍");
        TourPlaceItem duplicateFromSuncheonSearch = place("100", null);
        AtomicReference<AiTourPlaceCandidate> persisted = new AtomicReference<>();

        when(regionRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of(yeosu, suncheon));
        when(tourClient.findPlacesNearby(yeosu.getLatitude(), yeosu.getLongitude(), 20_000, 1, 30))
                .thenReturn(new TourPlaceMapResponse(1, 30, 1, List.of(yeosuResult)));
        when(tourClient.findPlacesNearby(suncheon.getLatitude(), suncheon.getLongitude(), 20_000, 1, 30))
                .thenReturn(new TourPlaceMapResponse(1, 30, 1, List.of(duplicateFromSuncheonSearch)));
        when(placeRepository.findBySourceAndExternalId(ExternalCandidateSource.TOUR_INFO, "100"))
                .thenAnswer(invocation -> Optional.ofNullable(persisted.get()));
        when(placeRepository.save(any(AiTourPlaceCandidate.class))).thenAnswer(invocation -> {
            AiTourPlaceCandidate candidate = invocation.getArgument(0);
            persisted.set(candidate);
            return candidate;
        });
        when(placeRepository.findAllBySourceAndActiveTrueAndLastSeenAtBefore(any(), any()))
                .thenReturn(List.of());
        when(embeddingClient.embed(anyList())).thenAnswer(invocation -> {
            List<String> inputs = invocation.getArgument(0);
            return inputs.stream().map(input -> new float[1536]).toList();
        });
        AiCandidateSyncService service = new AiCandidateSyncService(tourClient,
                mock(JunnamPublicJobApiClient.class), mock(ExternalJobService.class), regionRepository,
                placeRepository, mock(AiJobCandidateRepository.class), embeddingClient,
                new OpenAiProperties("key", "model", "https://example.com", "embedding", 1536),
                Clock.fixed(Instant.parse("2026-09-03T00:00:00Z"), ZoneOffset.UTC));

        service.syncTourPlaces();

        assertThat(persisted.get().getRegion()).isSameAs(yeosu);
    }

    @Test
    void invalidatesOldEmbeddingBeforeChangedCandidateEmbeddingFails() {
        TourInfoClient tourClient = mock(TourInfoClient.class);
        RegionRepository regionRepository = mock(RegionRepository.class);
        AiTourPlaceCandidateRepository placeRepository = mock(AiTourPlaceCandidateRepository.class);
        OpenAiEmbeddingClient embeddingClient = mock(OpenAiEmbeddingClient.class);
        Region yeosu = Region.createSupportedCity("여수", new BigDecimal("34.7604"), new BigDecimal("127.6622"));
        AiTourPlaceCandidate existing = AiTourPlaceCandidate.create("100", yeosu, "이전 이름",
                "A01", "전라남도 여수시", null, null,
                new BigDecimal("34.7"), new BigDecimal("127.7"),
                java.time.OffsetDateTime.parse("2026-09-02T00:00:00Z"));
        existing.updateEmbedding(new float[1536], "embedding", "old-hash",
                java.time.OffsetDateTime.parse("2026-09-02T00:00:00Z"));
        TourPlaceItem changed = new TourPlaceItem("100", "12", "A01", "A0101", "A01010100",
                "변경된 이름", "전라남도 여수시", null,
                new BigDecimal("127.7"), new BigDecimal("34.7"), null);

        when(regionRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of(yeosu));
        when(tourClient.findPlacesNearby(yeosu.getLatitude(), yeosu.getLongitude(), 20_000, 1, 30))
                .thenReturn(new TourPlaceMapResponse(1, 30, 1, List.of(changed)));
        when(placeRepository.findBySourceAndExternalId(ExternalCandidateSource.TOUR_INFO, "100"))
                .thenReturn(Optional.of(existing));
        when(placeRepository.save(any(AiTourPlaceCandidate.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(embeddingClient.embed(anyList())).thenThrow(new RuntimeException("embedding unavailable"));
        AiCandidateSyncService service = service(tourClient, regionRepository, placeRepository, embeddingClient);

        assertThatThrownBy(service::syncTourPlaces).isInstanceOf(RuntimeException.class);
        assertThat(existing.getEmbedding()).isNull();
    }

    @Test
    void skipsDeactivationWhenTourPlacePaginationEndsWithUnexpectedEmptyPage() {
        TourInfoClient tourClient = mock(TourInfoClient.class);
        RegionRepository regionRepository = mock(RegionRepository.class);
        AiTourPlaceCandidateRepository placeRepository = mock(AiTourPlaceCandidateRepository.class);
        OpenAiEmbeddingClient embeddingClient = mock(OpenAiEmbeddingClient.class);
        Region yeosu = Region.createSupportedCity("여수", new BigDecimal("34.7604"), new BigDecimal("127.6622"));

        when(regionRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of(yeosu));
        when(tourClient.findPlacesNearby(yeosu.getLatitude(), yeosu.getLongitude(), 20_000, 1, 30))
                .thenReturn(new TourPlaceMapResponse(1, 30, 31, List.of(place("100", "전라남도 여수시"))));
        when(tourClient.findPlacesNearby(yeosu.getLatitude(), yeosu.getLongitude(), 20_000, 2, 30))
                .thenReturn(new TourPlaceMapResponse(2, 30, 31, List.of()));
        when(placeRepository.findBySourceAndExternalId(any(), any())).thenReturn(Optional.empty());
        when(placeRepository.save(any(AiTourPlaceCandidate.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(embeddingClient.embed(anyList())).thenAnswer(invocation -> {
            List<String> inputs = invocation.getArgument(0);
            return inputs.stream().map(input -> new float[1536]).toList();
        });
        AiCandidateSyncService service = service(tourClient, regionRepository, placeRepository, embeddingClient);

        service.syncTourPlaces();

        verify(placeRepository, never()).findAllBySourceAndActiveTrueAndLastSeenAtBefore(any(), any());
    }

    private AiCandidateSyncService service(TourInfoClient tourClient, RegionRepository regionRepository,
                                           AiTourPlaceCandidateRepository placeRepository,
                                           OpenAiEmbeddingClient embeddingClient) {
        return new AiCandidateSyncService(tourClient, mock(JunnamPublicJobApiClient.class),
                mock(ExternalJobService.class), regionRepository, placeRepository,
                mock(AiJobCandidateRepository.class), embeddingClient,
                new OpenAiProperties("key", "model", "https://example.com", "embedding", 1536),
                Clock.fixed(Instant.parse("2026-09-03T00:00:00Z"), ZoneOffset.UTC));
    }

    private TourPlaceItem place(String contentId, String address) {
        BigDecimal mapX = new BigDecimal("127.7");
        BigDecimal mapY = new BigDecimal("34.7");
        return new TourPlaceItem(contentId, "12", "A01", "A0101", "A01010100",
                "중복 관광지", address, null, mapX, mapY, null);
    }
}
