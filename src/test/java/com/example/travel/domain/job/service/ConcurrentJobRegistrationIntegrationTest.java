package com.example.travel.domain.job.service;

import com.example.travel.domain.job.dto.CreateJobApplicationRequest;
import com.example.travel.domain.job.dto.CreateJobFavoriteRequest;
import com.example.travel.domain.job.enums.JobFavoriteSource;
import com.example.travel.domain.job.repository.JobApplicationRepository;
import com.example.travel.domain.job.repository.JobFavoriteRepository;
import com.example.travel.domain.user.entity.User;
import com.example.travel.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:concurrent-job-registration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ConcurrentJobRegistrationIntegrationTest {
    @Autowired JobApplicationService applicationService;
    @Autowired JobFavoriteService favoriteService;
    @Autowired JobApplicationRepository applicationRepository;
    @Autowired JobFavoriteRepository favoriteRepository;
    @Autowired UserRepository userRepository;

    @Test
    void concurrentApplicationRegistrationReturnsSameIdAndLeavesOneRow() throws Exception {
        Long userId = userRepository.saveAndFlush(User.create("app-user")).getId();
        var request = new CreateJobApplicationRequest(JobFavoriteSource.TOUR_JOB, "concurrent-app",
                "관광 일자리", "회사", null, null, null);

        var ids = runConcurrently(
                () -> applicationService.add(userId, request).applicationId(),
                () -> applicationService.add(userId, request).applicationId());

        assertThat(ids.first()).isEqualTo(ids.second());
        assertThat(applicationRepository.count()).isOne();
    }

    @Test
    void concurrentFavoriteRegistrationReturnsSameIdAndLeavesOneRow() throws Exception {
        Long userId = userRepository.saveAndFlush(User.create("fav-user")).getId();
        var request = new CreateJobFavoriteRequest(JobFavoriteSource.TOUR_JOB, "concurrent-fav",
                "관광 일자리", "회사", null, null, null);

        var ids = runConcurrently(
                () -> favoriteService.add(userId, request).favoriteId(),
                () -> favoriteService.add(userId, request).favoriteId());

        assertThat(ids.first()).isEqualTo(ids.second());
        assertThat(favoriteRepository.count()).isOne();
    }

    private Pair runConcurrently(java.util.concurrent.Callable<Long> first,
                                 java.util.concurrent.Callable<Long> second) throws Exception {
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            Future<Long> firstResult = executor.submit(() -> { start.await(); return first.call(); });
            Future<Long> secondResult = executor.submit(() -> { start.await(); return second.call(); });
            start.countDown();
            return new Pair(firstResult.get(), secondResult.get());
        } finally {
            executor.shutdownNow();
        }
    }

    private record Pair(Long first, Long second) {}
}
