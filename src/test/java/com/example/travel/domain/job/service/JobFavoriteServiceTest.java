package com.example.travel.domain.job.service;

import com.example.travel.domain.job.dto.CreateJobFavoriteRequest;
import com.example.travel.domain.job.entity.JobFavorite;
import com.example.travel.domain.job.enums.JobFavoriteSource;
import com.example.travel.domain.job.exception.JobFavoriteException;
import com.example.travel.domain.job.repository.JobFavoriteRepository;
import com.example.travel.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JobFavoriteServiceTest {
    @Test
    void addingSameExternalJobIsIdempotent() {
        JobFavoriteRepository repository = mock(JobFavoriteRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        JobFavorite existing = mock(JobFavorite.class);
        when(existing.getId()).thenReturn(11L);
        when(existing.getSource()).thenReturn(JobFavoriteSource.TOUR_JOB);
        when(existing.getExternalId()).thenReturn("tour-1");
        when(existing.getTitle()).thenReturn("관광 일자리");
        when(repository.findByUserIdAndSourceAndExternalId(7L, JobFavoriteSource.TOUR_JOB, "tour-1"))
                .thenReturn(Optional.of(existing));
        JobFavoriteService service = new JobFavoriteService(repository, userRepository);

        var response = service.add(7L, request());

        assertThat(response.favoriteId()).isEqualTo(11L);
        verify(repository, never()).save(any());
        verifyNoInteractions(userRepository);
    }

    @Test
    void listsOnlyCurrentUsersFavorites() {
        JobFavoriteRepository repository = mock(JobFavoriteRepository.class);
        when(repository.findAllByUserIdOrderByIdDesc(eq(7L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        JobFavoriteService service = new JobFavoriteService(repository, mock(UserRepository.class));

        var response = service.findAll(7L, 0, 20);

        assertThat(response.content()).isEmpty();
        verify(repository).findAllByUserIdOrderByIdDesc(eq(7L), argThat(pageable ->
                pageable.getPageNumber() == 0 && pageable.getPageSize() == 20));
    }

    @Test
    void cannotDeleteAnotherUsersFavorite() {
        JobFavoriteRepository repository = mock(JobFavoriteRepository.class);
        when(repository.findByIdAndUserId(11L, 7L)).thenReturn(Optional.empty());
        JobFavoriteService service = new JobFavoriteService(repository, mock(UserRepository.class));

        assertThatThrownBy(() -> service.delete(7L, 11L))
                .isInstanceOfSatisfying(JobFavoriteException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("JOB_FAVORITE_404_NOT_FOUND"));
    }

    private CreateJobFavoriteRequest request() {
        return new CreateJobFavoriteRequest(JobFavoriteSource.TOUR_JOB, "tour-1", "관광 일자리",
                "회사", "전라남도 여수시", "2026-09-30", "https://example.com/jobs/1");
    }
}
