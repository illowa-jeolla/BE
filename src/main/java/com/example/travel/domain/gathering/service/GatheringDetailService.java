package com.example.travel.domain.gathering.service;

import com.example.travel.domain.gathering.dto.GatheringDetailCandidate;
import com.example.travel.domain.gathering.dto.GatheringDetailResponse;
import com.example.travel.domain.gathering.enums.ParticipantStatus;
import com.example.travel.domain.gathering.exception.GatheringErrorCode;
import com.example.travel.domain.gathering.exception.GatheringException;
import com.example.travel.domain.gathering.repository.GatheringRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GatheringDetailService {
    private final GatheringRepository gatheringRepository;

    public GatheringDetailService(GatheringRepository gatheringRepository) {
        this.gatheringRepository = gatheringRepository;
    }

    @Transactional(readOnly = true)
    public GatheringDetailResponse findDetail(Long gatheringId, Long userId) {
        GatheringDetailCandidate candidate = gatheringRepository.findDetail(
                        gatheringId, userId, ParticipantStatus.JOINED)
                .orElseThrow(() -> new GatheringException(
                        GatheringErrorCode.GATHERING_NOT_FOUND));

        return new GatheringDetailResponse(
                candidate.id(), candidate.title(), candidate.description(),
                new GatheringDetailResponse.RegionSummary(
                        candidate.regionId(), candidate.regionName()),
                candidate.concept(), candidate.meetingPlace(), candidate.latitude(),
                candidate.longitude(), candidate.startsAt(), candidate.capacity(),
                candidate.participantCount(), candidate.status(), candidate.joinedCount() > 0,
                candidate.creatorId().equals(userId),
                new GatheringDetailResponse.CreatorSummary(candidate.creatorId(),
                        candidate.creatorNickname(), candidate.creatorAvatarUrl()),
                candidate.createdAt(), candidate.updatedAt());
    }
}
