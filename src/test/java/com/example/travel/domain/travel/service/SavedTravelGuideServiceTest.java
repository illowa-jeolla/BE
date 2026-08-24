package com.example.travel.domain.travel.service;

import com.example.travel.domain.region.entity.Region;
import com.example.travel.domain.travel.entity.SavedTravelGuide;
import com.example.travel.domain.travel.entity.TravelGuide;
import com.example.travel.domain.travel.entity.TravelRecommendationRequest;
import com.example.travel.domain.travel.entity.TravelGuideCondition;
import com.example.travel.domain.travel.entity.id.SavedTravelGuideId;
import com.example.travel.domain.travel.dto.response.TravelGuideDraft;
import com.example.travel.domain.travel.exception.TravelRecommendationErrorCode;
import com.example.travel.domain.travel.exception.TravelRecommendationException;
import com.example.travel.domain.travel.repository.SavedTravelGuideRepository;
import com.example.travel.domain.travel.repository.TravelGuideRepository;
import com.example.travel.domain.user.entity.User;
import com.example.travel.domain.user.enums.UserStatus;
import com.example.travel.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SavedTravelGuideServiceTest {
    @Mock private SavedTravelGuideRepository savedGuideRepository;
    @Mock private TravelGuideRepository guideRepository;
    @Mock private UserRepository userRepository;
    @Mock private TravelGuideDraftCacheService draftCacheService;
    @Mock private TravelRecommendationPersistenceService persistenceService;
    @Mock private User user;
    @Mock private TravelGuide guide;
    @Mock private SavedTravelGuide savedGuide;
    @Mock private TravelRecommendationRequest request;
    @Mock private Region region;
    @Mock private TravelGuideCondition condition;

    private SavedTravelGuideService service;

    @BeforeEach
    void setUp() {
        service = new SavedTravelGuideService(savedGuideRepository, guideRepository,
                userRepository, Clock.fixed(Instant.parse("2026-08-20T01:00:00Z"),
                ZoneOffset.UTC), draftCacheService, persistenceService);
    }

    @Test
    void restoresSoftDeletedGuide() {
        SavedTravelGuideId id = new SavedTravelGuideId(1L, 10L);
        when(guideRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(guide));
        when(savedGuideRepository.findById(id)).thenReturn(Optional.of(savedGuide));
        when(savedGuide.getDeletedAt()).thenReturn(OffsetDateTime.parse("2026-08-19T01:00:00Z"));

        var response = service.restore(1L, 10L);

        assertThat(response.guideId()).isEqualTo(10L);
        assertThat(response.saved()).isTrue();
        verify(savedGuide).restore();
    }

    @Test
    void rejectsRestoringActiveSavedGuide() {
        SavedTravelGuideId id = new SavedTravelGuideId(1L, 10L);
        when(guideRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(guide));
        when(savedGuideRepository.findById(id)).thenReturn(Optional.of(savedGuide));
        when(savedGuide.getDeletedAt()).thenReturn(null);

        assertThatThrownBy(() -> service.restore(1L, 10L))
                .isInstanceOf(TravelRecommendationException.class)
                .extracting("code")
                .isEqualTo(TravelRecommendationErrorCode.GUIDE_ALREADY_SAVED.code());
    }

    @Test
    void rejectsGuideOwnedByAnotherUser() {
        when(guideRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.restore(1L, 10L))
                .isInstanceOf(TravelRecommendationException.class)
                .extracting("code")
                .isEqualTo(TravelRecommendationErrorCode.GUIDE_NOT_FOUND.code());
    }

    @Test
    void rejectsCancelWhenAlreadyDeleted() {
        SavedTravelGuideId id = new SavedTravelGuideId(1L, 10L);
        when(guideRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(guide));
        when(savedGuideRepository.findById(id)).thenReturn(Optional.of(savedGuide));
        when(savedGuide.getDeletedAt()).thenReturn(OffsetDateTime.parse("2026-08-19T01:00:00Z"));

        assertThatThrownBy(() -> service.cancel(1L, 10L))
                .isInstanceOf(TravelRecommendationException.class)
                .extracting("code")
                .isEqualTo(TravelRecommendationErrorCode.GUIDE_ALREADY_DELETED.code());
    }

    @Test
    void softDeletesSavedGuide() {
        SavedTravelGuideId id = new SavedTravelGuideId(1L, 10L);
        when(guideRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(guide));
        when(savedGuideRepository.findById(id)).thenReturn(Optional.of(savedGuide));
        when(savedGuide.getDeletedAt()).thenReturn(null);

        assertThat(service.cancel(1L, 10L).saved()).isFalse();

        verify(savedGuide).delete(OffsetDateTime.parse("2026-08-20T01:00:00Z"));
        verify(savedGuideRepository, never()).deleteById(id);
    }

    @Test
    void returnsSavedGuideSummaries() {
        OffsetDateTime savedAt = OffsetDateTime.parse("2026-08-20T10:00:00+09:00");
        when(savedGuideRepository.findByUserIdAndDeletedAtIsNullOrderBySavedAtDesc(1L))
                .thenReturn(List.of(savedGuide));
        when(savedGuide.getGuide()).thenReturn(guide);
        when(savedGuide.getSavedAt()).thenReturn(savedAt);
        when(guide.getId()).thenReturn(10L);
        when(guide.getTitle()).thenReturn("완도 힐링 여행");
        when(guide.getSummary()).thenReturn("바다 중심 일정");
        when(guide.isGeneratedByAi()).thenReturn(true);
        when(guide.getCondition()).thenReturn(condition);
        when(condition.getRegionName()).thenReturn("완도");
        when(condition.getStartsOn()).thenReturn(LocalDate.of(2026, 8, 20));
        when(condition.getEndsOn()).thenReturn(LocalDate.of(2026, 8, 22));

        var result = service.findAll(1L);

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.guideId()).isEqualTo(10L);
            assertThat(item.regionName()).isEqualTo("완도");
            assertThat(item.savedAt()).isEqualTo(savedAt);
        });
    }

    @Test
    void returnsSoftDeletedGuidesForRestore() {
        OffsetDateTime deletedAt = OffsetDateTime.parse("2026-08-20T10:00:00+09:00");
        when(savedGuideRepository.findByUserIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(1L))
                .thenReturn(List.of(savedGuide));
        when(savedGuide.getGuide()).thenReturn(guide);
        when(savedGuide.getDeletedAt()).thenReturn(deletedAt);
        when(guide.getId()).thenReturn(10L);
        when(guide.getTitle()).thenReturn("완도 여행");
        when(guide.getSummary()).thenReturn("바다 중심 일정");
        when(guide.isGeneratedByAi()).thenReturn(true);
        when(guide.getCondition()).thenReturn(condition);
        when(condition.getRegionName()).thenReturn("완도");
        when(condition.getStartsOn()).thenReturn(LocalDate.of(2026, 8, 20));
        when(condition.getEndsOn()).thenReturn(LocalDate.of(2026, 8, 22));

        var result = service.findDeleted(1L);

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.guideId()).isEqualTo(10L);
            assertThat(item.regionName()).isEqualTo("완도");
            assertThat(item.deletedAt()).isEqualTo(deletedAt);
        });
    }

    @Test
    void promotesRedisDraftAndSavesItPermanently() {
        TravelGuideDraft draft = org.mockito.Mockito.mock(TravelGuideDraft.class);
        when(draftCacheService.find(30L)).thenReturn(draft);
        when(draft.userId()).thenReturn(1L);
        when(draft.request()).thenReturn(request);
        when(draft.result()).thenReturn(org.mockito.Mockito.mock(
                com.example.travel.domain.travel.ai.dto.AiTravelGuideResult.class));
        when(draft.candidates()).thenReturn(List.of());
        when(draft.routes()).thenReturn(List.of());
        when(persistenceService.saveGuide(org.mockito.ArgumentMatchers.eq(request),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(List.of()),
                org.mockito.ArgumentMatchers.eq(List.of()), org.mockito.ArgumentMatchers.eq(false)))
                .thenReturn(40L);
        when(guideRepository.findByIdAndUserId(40L, 1L)).thenReturn(Optional.of(guide));
        when(guide.getId()).thenReturn(40L);
        when(userRepository.findByIdAndStatus(1L, UserStatus.ACTIVE))
                .thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(1L);
        when(savedGuideRepository.findById(new SavedTravelGuideId(1L, 40L)))
                .thenReturn(Optional.empty());

        TransactionSynchronizationManager.initSynchronization();
        try {
            var response = service.saveDraft(1L, 30L);

            assertThat(response.guideId()).isEqualTo(40L);
            assertThat(response.saved()).isTrue();
            verify(savedGuideRepository).save(
                    org.mockito.ArgumentMatchers.any(SavedTravelGuide.class));
            verify(draftCacheService, never()).delete(30L, 1L);

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(synchronization -> synchronization.afterCommit());
            verify(draftCacheService).delete(30L, 1L);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void rejectsSavingSameDraftTwice() {
        when(guideRepository.findBySourceRequestIdAndUserId(30L, 1L))
                .thenReturn(Optional.of(guide));

        assertThatThrownBy(() -> service.saveDraft(1L, 30L))
                .isInstanceOf(TravelRecommendationException.class)
                .extracting("code")
                .isEqualTo(TravelRecommendationErrorCode.GUIDE_ALREADY_SAVED.code());

        verify(draftCacheService, never()).find(30L);
    }
}
