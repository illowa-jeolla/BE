package com.example.travel.domain.gathering.service;

import com.example.travel.domain.gathering.dto.CreateGatheringRequest;
import com.example.travel.domain.gathering.dto.CreateGatheringResponse;
import com.example.travel.domain.gathering.entity.Gathering;
import com.example.travel.domain.gathering.entity.GatheringParticipant;
import com.example.travel.domain.gathering.exception.GatheringErrorCode;
import com.example.travel.domain.gathering.exception.GatheringException;
import com.example.travel.domain.gathering.repository.GatheringParticipantRepository;
import com.example.travel.domain.gathering.repository.GatheringRepository;
import com.example.travel.domain.region.entity.Region;
import com.example.travel.domain.region.repository.RegionRepository;
import com.example.travel.domain.user.entity.User;
import com.example.travel.domain.user.enums.UserStatus;
import com.example.travel.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

@Service
public class GatheringService {
    private final UserRepository userRepository;
    private final RegionRepository regionRepository;
    private final GatheringRepository gatheringRepository;
    private final GatheringParticipantRepository participantRepository;
    private final Clock clock;

    public GatheringService(UserRepository userRepository, RegionRepository regionRepository,
                            GatheringRepository gatheringRepository,
                            GatheringParticipantRepository participantRepository,
                            Clock clock) {
        this.userRepository = userRepository;
        this.regionRepository = regionRepository;
        this.gatheringRepository = gatheringRepository;
        this.participantRepository = participantRepository;
        this.clock = clock;
    }

    @Transactional
    public CreateGatheringResponse create(Long userId, CreateGatheringRequest request) {
        User creator = userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new GatheringException(GatheringErrorCode.USER_NOT_FOUND));
        Region region = regionRepository.findActiveByName(request.region().trim())
                .orElseThrow(() -> new GatheringException(GatheringErrorCode.REGION_NOT_FOUND));

        if (!request.startsAt().isAfter(OffsetDateTime.now(clock))) {
            throw new GatheringException(GatheringErrorCode.INVALID_START_TIME);
        }

        Gathering gathering = Gathering.create(creator, region, request.title().trim(),
                request.capacity().shortValue(), request.meetingPlace().trim(), request.startsAt(),
                request.concept().trim(), request.description().trim());
        Gathering saved = gatheringRepository.save(gathering);
        participantRepository.save(GatheringParticipant.createHost(saved, creator));

        return new CreateGatheringResponse(saved.getId(), saved.getStatus());
    }
}
