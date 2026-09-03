package com.example.travel.domain.ai.service;

import com.example.travel.domain.ai.dto.request.CreateAiMatchRequest;
import com.example.travel.domain.ai.enums.AiRequestStatus;
import com.example.travel.domain.ai.enums.PriorityType;
import com.example.travel.domain.ai.exception.AiMatchException;
import com.example.travel.domain.ai.model.AiMatchRequestContext;
import com.example.travel.domain.region.entity.Region;
import com.example.travel.domain.region.repository.RegionRepository;
import com.example.travel.domain.user.entity.User;
import com.example.travel.domain.user.enums.UserStatus;
import com.example.travel.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AiMatchServiceTest {
    private final AiMatchRequestCacheService cache = mock(AiMatchRequestCacheService.class);
    private final RegionRepository regions = mock(RegionRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final AiMatchDailyLimitService dailyLimit = mock(AiMatchDailyLimitService.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-01T03:00:00Z"), ZoneOffset.UTC);
    private final AiMatchService service = new AiMatchService(cache, regions, users, dailyLimit, publisher, clock);

    @Test
    void storesInputOnlyInRedisAndPublishesProcessingEvent() {
        Region region = Region.createSupportedCity("여수", new BigDecimal("34.7604"), new BigDecimal("127.6622"));
        when(regions.findActiveById(3L)).thenReturn(Optional.of(region));
        when(users.findByIdAndStatus(7L, UserStatus.ACTIVE)).thenReturn(Optional.of(User.create("여행자")));

        var response = service.create(7L, request(List.of(
                PriorityType.JOB, PriorityType.TOURISM, PriorityType.HOUSING, PriorityType.COMMUNITY)));

        assertThat(response.status()).isEqualTo(AiRequestStatus.PROCESSING);
        ArgumentCaptor<AiMatchRequestContext> context = ArgumentCaptor.forClass(AiMatchRequestContext.class);
        verify(cache).save(context.capture());
        assertThat(context.getValue().thought()).isEqualTo("바다 근처에서 관광 일을 하고 싶어요.");
        assertThat(context.getValue().userId()).isEqualTo(7L);
        verify(dailyLimit).acquire(7L);
        verify(publisher).publishEvent(any(AiMatchCreatedEvent.class));
    }

    @Test
    void rejectsDuplicatedPrioritiesBeforeWritingRedis() {
        assertThatThrownBy(() -> service.create(7L, request(List.of(
                PriorityType.JOB, PriorityType.JOB, PriorityType.HOUSING, PriorityType.COMMUNITY))))
                .isInstanceOf(AiMatchException.class);
        verifyNoInteractions(cache, publisher, regions, users, dailyLimit);
    }

    @Test
    void doesNotCreateRequestWhenDailyLimitIsExceeded() {
        Region region = Region.createSupportedCity("여수", new BigDecimal("34.7604"), new BigDecimal("127.6622"));
        when(regions.findActiveById(3L)).thenReturn(Optional.of(region));
        when(users.findByIdAndStatus(7L, UserStatus.ACTIVE)).thenReturn(Optional.of(User.create("여행자")));
        doThrow(new AiMatchException(com.example.travel.domain.ai.exception.AiMatchErrorCode.DAILY_LIMIT_EXCEEDED))
                .when(dailyLimit).acquire(7L);

        assertThatThrownBy(() -> service.create(7L, request(List.of(
                PriorityType.JOB, PriorityType.TOURISM, PriorityType.HOUSING, PriorityType.COMMUNITY))))
                .isInstanceOf(AiMatchException.class);

        verifyNoInteractions(cache, publisher);
    }

    private CreateAiMatchRequest request(List<PriorityType> priorities) {
        return new CreateAiMatchRequest(3L, List.of("관광 안내"), priorities,
                "바다 근처에서 관광 일을 하고 싶어요.");
    }
}
