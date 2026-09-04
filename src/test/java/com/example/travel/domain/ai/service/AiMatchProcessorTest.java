package com.example.travel.domain.ai.service;

import com.example.travel.domain.ai.client.OpenAiClient;
import com.example.travel.domain.ai.client.OpenAiEmbeddingClient;
import com.example.travel.domain.ai.dto.response.AiMatchResultResponse;
import com.example.travel.domain.ai.entity.AiJobCandidate;
import com.example.travel.domain.ai.entity.AiTourPlaceCandidate;
import com.example.travel.domain.ai.enums.AiRequestStatus;
import com.example.travel.domain.ai.enums.ExternalCandidateSource;
import com.example.travel.domain.ai.enums.PriorityType;
import com.example.travel.domain.ai.model.AiMatchRequestContext;
import com.example.travel.domain.ai.repository.AiCandidateSearchRepository;
import com.example.travel.domain.region.entity.Region;
import com.example.travel.domain.region.repository.RegionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiMatchProcessorTest {

    @Test
    void replacesMissingRegionalJobsWithJobsFromAnotherRegion() {
        AiMatchRequestCacheService cache = mock(AiMatchRequestCacheService.class);
        OpenAiEmbeddingClient embeddingClient = mock(OpenAiEmbeddingClient.class);
        AiCandidateSearchRepository searchRepository = mock(AiCandidateSearchRepository.class);
        AiMatchPromptService promptService = mock(AiMatchPromptService.class);
        OpenAiClient openAiClient = mock(OpenAiClient.class);
        RegionRepository regionRepository = mock(RegionRepository.class);
        AiMatchPersistenceService persistenceService = mock(AiMatchPersistenceService.class);
        AiMatchProcessor processor = new AiMatchProcessor(cache, embeddingClient, searchRepository,
                promptService, openAiClient, regionRepository, persistenceService);

        UUID requestId = UUID.randomUUID();
        AiMatchRequestContext context = new AiMatchRequestContext(requestId, 1L, 7L,
                List.of("관광 기획"),
                List.of(PriorityType.JOB, PriorityType.HOUSING,
                        PriorityType.TOURISM, PriorityType.COMMUNITY),
                "바다 근처에서 일하고 싶어요.", AiRequestStatus.PROCESSING,
                OffsetDateTime.now(), null);
        Region region = mock(Region.class);
        AiJobCandidate job = mock(AiJobCandidate.class);
        AiTourPlaceCandidate place = mock(AiTourPlaceCandidate.class);

        when(cache.find(requestId)).thenReturn(Optional.of(context));
        when(regionRepository.findActiveById(7L)).thenReturn(Optional.of(region));
        when(region.getId()).thenReturn(7L);
        when(region.getName()).thenReturn("여수");
        when(embeddingClient.embedOne(anyString())).thenReturn(new float[]{0.1f});
        when(searchRepository.findJobs(eq(7L), any(float[].class), eq(20))).thenReturn(List.of());
        when(searchRepository.findJobsAcrossRegions(any(float[].class), eq(20))).thenReturn(List.of(
                new AiCandidateSearchRepository.JobMatch(job, 0.85, 8L, "보성")));
        when(job.getId()).thenReturn(4L);
        when(job.getSource()).thenReturn(ExternalCandidateSource.JUNNAM_PUBLIC_JOB);
        when(job.getExternalId()).thenReturn("8578");
        when(job.getTitle()).thenReturn("해양 관광 콘텐츠 기획자");
        when(searchRepository.findPlaces(eq(7L), any(float[].class), eq(20))).thenReturn(List.of(
                new AiCandidateSearchRepository.PlaceMatch(place, 0.9)));
        when(place.getId()).thenReturn(3L);
        when(place.getExternalId()).thenReturn("1276");
        when(place.getName()).thenReturn("여수 해상케이블카");
        when(promptService.instructions()).thenReturn("instructions");
        when(promptService.searchText(context)).thenReturn("search text");
        when(promptService.input(eq(context), eq(region), anyList(), anyList())).thenReturn("input");
        when(promptService.schema()).thenReturn(new ObjectMapper().createObjectNode());
        when(openAiClient.generateStructured(anyString(), eq("input"), any())).thenReturn("""
                {"summary":"관광지 추천", "housingScore":80, "communityScore":70,
                 "jobs":[{"id":4,"reason":"첫 번째 이유"},{"id":4,"reason":"중복 이유"}],
                 "places":[{"id":3,"reason":"첫 번째 이유"},{"id":3,"reason":"중복 이유"}]}
                """);

        processor.process(requestId);

        ArgumentCaptor<AiMatchResultResponse> response = ArgumentCaptor.forClass(AiMatchResultResponse.class);
        verify(persistenceService).save(eq(requestId), eq(1L), response.capture());
        assertThat(response.getValue().status()).isEqualTo(AiRequestStatus.REPLACED);
        assertThat(response.getValue().results().get(0).jobStatus().status())
                .isEqualTo(AiMatchResultResponse.SectionState.REPLACED);
        assertThat(response.getValue().results().get(0).jobStatus().message())
                .isEqualTo("선택한 지역에 추천 가능한 일자리가 없어 다른 지역의 일자리로 대체했습니다.");
        assertThat(response.getValue().results().get(0).jobs()).hasSize(1);
        assertThat(response.getValue().results().get(0).jobs().get(0).matchScore()).isEqualTo(85);
        assertThat(response.getValue().results().get(0).jobs().get(0).reason()).isEqualTo("첫 번째 이유");
        assertThat(response.getValue().results().get(0).jobs().get(0).region().name()).isEqualTo("보성");
        assertThat(response.getValue().results().get(0).tourismStatus().status())
                .isEqualTo(AiMatchResultResponse.SectionState.SUCCESS);
        assertThat(response.getValue().results().get(0).places()).hasSize(1);
        assertThat(response.getValue().results().get(0).places().get(0).matchScore()).isEqualTo(90);
        assertThat(response.getValue().results().get(0).places().get(0).reason()).isEqualTo("첫 번째 이유");
        verify(cache).delete(requestId);
    }
}
