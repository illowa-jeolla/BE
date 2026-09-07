package com.example.travel.domain.job.service;

import com.example.travel.domain.job.dto.CreateJobFavoriteRequest;
import com.example.travel.domain.job.dto.JobFavoriteItem;
import com.example.travel.domain.job.dto.JobFavoriteListResponse;
import com.example.travel.domain.job.entity.JobFavorite;
import com.example.travel.domain.job.exception.JobFavoriteErrorCode;
import com.example.travel.domain.job.exception.JobFavoriteException;
import com.example.travel.domain.job.repository.JobFavoriteRepository;
import com.example.travel.domain.user.enums.UserStatus;
import com.example.travel.domain.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class JobFavoriteService {
    private final JobFavoriteRepository favoriteRepository;
    private final UserRepository userRepository;

    public JobFavoriteService(JobFavoriteRepository favoriteRepository, UserRepository userRepository) {
        this.favoriteRepository = favoriteRepository; this.userRepository = userRepository;
    }

    @Transactional
    public JobFavoriteItem add(Long userId, CreateJobFavoriteRequest request) {
        var existing = favoriteRepository.findByUserIdAndSourceAndExternalId(
                userId, request.source(), request.externalId().trim());
        if (existing.isPresent()) return JobFavoriteItem.from(existing.get());

        var user = userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new JobFavoriteException(JobFavoriteErrorCode.USER_NOT_FOUND));
        JobFavorite favorite = JobFavorite.create(user, request.source(), request.externalId().trim(),
                request.title().trim(), normalize(request.companyName()), normalize(request.address()),
                normalize(request.deadline()), normalize(request.sourceUrl()));
        return JobFavoriteItem.from(favoriteRepository.save(favorite));
    }

    public JobFavoriteListResponse findAll(Long userId, int page, int size) {
        var result = favoriteRepository.findAllByUserIdOrderByIdDesc(userId, PageRequest.of(page, size));
        return new JobFavoriteListResponse(result.getContent().stream().map(JobFavoriteItem::from).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.hasNext());
    }

    @Transactional
    public void delete(Long userId, Long favoriteId) {
        JobFavorite favorite = favoriteRepository.findByIdAndUserId(favoriteId, userId)
                .orElseThrow(() -> new JobFavoriteException(JobFavoriteErrorCode.NOT_FOUND));
        favoriteRepository.delete(favorite);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
