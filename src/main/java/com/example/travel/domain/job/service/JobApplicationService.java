package com.example.travel.domain.job.service;

import com.example.travel.domain.job.dto.*;
import com.example.travel.domain.job.entity.JobApplication;
import com.example.travel.domain.job.exception.JobApplicationErrorCode;
import com.example.travel.domain.job.exception.JobApplicationException;
import com.example.travel.domain.job.repository.JobApplicationRepository;
import com.example.travel.domain.user.enums.UserStatus;
import com.example.travel.domain.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class JobApplicationService {
    private final JobApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    public JobApplicationService(JobApplicationRepository applicationRepository,
                                 UserRepository userRepository) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public JobApplicationItem add(Long userId, CreateJobApplicationRequest request) {
        String externalId = request.externalId().trim();
        var user = userRepository.findByIdAndStatusForUpdate(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new JobApplicationException(JobApplicationErrorCode.USER_NOT_FOUND));
        var existing = applicationRepository.findByUserIdAndSourceAndExternalId(
                userId, request.source(), externalId);
        if (existing.isPresent()) {
            return JobApplicationItem.from(existing.get());
        }

        JobApplication application = JobApplication.create(user, request.source(), externalId,
                request.title().trim(), normalize(request.companyName()), normalize(request.address()),
                normalize(request.deadline()), normalize(request.sourceUrl()));
        return JobApplicationItem.from(applicationRepository.saveAndFlush(application));
    }

    public JobApplicationListResponse findAll(Long userId, int page, int size) {
        var result = applicationRepository.findAllByUserIdOrderByIdDesc(
                userId, PageRequest.of(page, size));
        return new JobApplicationListResponse(
                result.getContent().stream().map(JobApplicationItem::from).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.hasNext());
    }

    @Transactional
    public JobApplicationItem updateStatus(Long userId, Long applicationId,
                                           UpdateJobApplicationStatusRequest request) {
        JobApplication application = findOwned(applicationId, userId);
        application.updateStatus(request.status());
        return JobApplicationItem.from(application);
    }

    @Transactional
    public void delete(Long userId, Long applicationId) {
        applicationRepository.delete(findOwned(applicationId, userId));
    }

    private JobApplication findOwned(Long applicationId, Long userId) {
        return applicationRepository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new JobApplicationException(JobApplicationErrorCode.NOT_FOUND));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
