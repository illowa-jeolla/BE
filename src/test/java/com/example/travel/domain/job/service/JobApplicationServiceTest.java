package com.example.travel.domain.job.service;

import com.example.travel.domain.job.dto.CreateJobApplicationRequest;
import com.example.travel.domain.job.dto.UpdateJobApplicationStatusRequest;
import com.example.travel.domain.job.entity.JobApplication;
import com.example.travel.domain.job.enums.JobApplicationStatus;
import com.example.travel.domain.job.enums.JobFavoriteSource;
import com.example.travel.domain.job.exception.JobApplicationException;
import com.example.travel.domain.job.repository.JobApplicationRepository;
import com.example.travel.domain.user.repository.UserRepository;
import com.example.travel.domain.user.entity.User;
import com.example.travel.domain.user.enums.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JobApplicationServiceTest {
    @Test
    void addingSameExternalJobIsIdempotent() {
        JobApplicationRepository repository = mock(JobApplicationRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        JobApplication existing = mock(JobApplication.class);
        when(existing.getId()).thenReturn(11L);
        when(existing.getSource()).thenReturn(JobFavoriteSource.TOUR_JOB);
        when(existing.getExternalId()).thenReturn("tour-1");
        when(existing.getTitle()).thenReturn("관광 일자리");
        when(existing.getStatus()).thenReturn(JobApplicationStatus.APPLIED);
        when(repository.findByUserIdAndSourceAndExternalId(7L, JobFavoriteSource.TOUR_JOB, "tour-1"))
                .thenReturn(Optional.of(existing));
        when(userRepository.findByIdAndStatusForUpdate(7L, UserStatus.ACTIVE))
                .thenReturn(Optional.of(mock(User.class)));
        JobApplicationService service = new JobApplicationService(repository, userRepository);

        var response = service.add(7L, request());

        assertThat(response.applicationId()).isEqualTo(11L);
        verify(repository, never()).save(any());
        verify(userRepository).findByIdAndStatusForUpdate(7L, UserStatus.ACTIVE);
    }

    @Test
    void listsOnlyCurrentUsersApplications() {
        JobApplicationRepository repository = mock(JobApplicationRepository.class);
        when(repository.findAllByUserIdOrderByIdDesc(eq(7L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        JobApplicationService service = new JobApplicationService(repository, mock(UserRepository.class));

        var response = service.findAll(7L, 0, 20);

        assertThat(response.content()).isEmpty();
        verify(repository).findAllByUserIdOrderByIdDesc(eq(7L), argThat(pageable ->
                pageable.getPageNumber() == 0 && pageable.getPageSize() == 20));
    }

    @Test
    void updatesStatusOfOwnedApplication() {
        JobApplicationRepository repository = mock(JobApplicationRepository.class);
        JobApplication application = mock(JobApplication.class);
        when(application.getStatus()).thenReturn(JobApplicationStatus.INTERVIEW);
        when(repository.findByIdAndUserId(11L, 7L)).thenReturn(Optional.of(application));
        JobApplicationService service = new JobApplicationService(repository, mock(UserRepository.class));

        var response = service.updateStatus(7L, 11L,
                new UpdateJobApplicationStatusRequest(JobApplicationStatus.INTERVIEW));

        verify(application).updateStatus(JobApplicationStatus.INTERVIEW);
        assertThat(response.status()).isEqualTo(JobApplicationStatus.INTERVIEW);
    }

    @Test
    void cannotDeleteAnotherUsersApplication() {
        JobApplicationRepository repository = mock(JobApplicationRepository.class);
        when(repository.findByIdAndUserId(11L, 7L)).thenReturn(Optional.empty());
        JobApplicationService service = new JobApplicationService(repository, mock(UserRepository.class));

        assertThatThrownBy(() -> service.delete(7L, 11L))
                .isInstanceOfSatisfying(JobApplicationException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("JOB_APPLICATION_404_NOT_FOUND"));
    }

    private CreateJobApplicationRequest request() {
        return new CreateJobApplicationRequest(JobFavoriteSource.TOUR_JOB, "tour-1", "관광 일자리",
                "회사", "전라남도 여수시", "2026-09-30", "https://example.com/jobs/1");
    }
}
