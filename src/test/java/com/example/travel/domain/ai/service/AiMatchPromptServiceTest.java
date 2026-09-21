package com.example.travel.domain.ai.service;

import com.example.travel.domain.ai.config.AiMatchScoreProperties;
import com.example.travel.domain.ai.enums.AiRequestStatus;
import com.example.travel.domain.ai.enums.PriorityType;
import com.example.travel.domain.ai.model.AiMatchRequestContext;
import com.example.travel.domain.region.entity.Region;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiMatchPromptServiceTest {
    private final AiMatchPromptService service = new AiMatchPromptService(
            new AiMatchScoreCalculator(new AiMatchScoreProperties()));

    @Test
    void separatesJobAndLifestyleTourismSearchIntents() {
        AiMatchRequestContext context = new AiMatchRequestContext(UUID.randomUUID(), 1L, 7L,
                List.of("프로그래머"), List.of(PriorityType.JOB, PriorityType.HOUSING,
                PriorityType.TOURISM, PriorityType.COMMUNITY),
                "바다 가까이에서 산책하고 싶어요.", AiRequestStatus.PROCESSING,
                OffsetDateTime.now(), null);
        Region region = mock(Region.class);
        when(region.getName()).thenReturn("여수");

        assertThat(service.jobSearchText(context))
                .contains("프로그래머", "희망 직무", "근무 및 생활 조건");
        assertThat(service.tourismSearchText(context, region))
                .contains("여수", "생활관광", "바다 가까이")
                .doesNotContain("프로그래머");
        assertThat(service.instructions()).contains("70점", "최소 1개");
    }
}
