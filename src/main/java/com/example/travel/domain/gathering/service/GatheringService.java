package com.example.travel.domain.gathering.service;

import com.example.travel.domain.gathering.dto.request.CreateGatheringRequest;
import com.example.travel.domain.gathering.dto.response.CreateGatheringResponse;
import com.example.travel.domain.gathering.dto.request.UpdateGatheringRequest;
import com.example.travel.domain.gathering.dto.response.UpdateGatheringResponse;
import com.example.travel.domain.gathering.entity.Gathering;
import com.example.travel.domain.gathering.entity.GatheringParticipant;
import com.example.travel.domain.gathering.enums.GatheringStatus;
import com.example.travel.domain.gathering.enums.ParticipantStatus;
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

    @Transactional
    public UpdateGatheringResponse update(Long gatheringId, Long userId,
                                          UpdateGatheringRequest request) {
        Gathering gathering = findForHost(gatheringId, userId);
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (!gathering.getStartsAt().isAfter(now)) {
            throw new GatheringException(GatheringErrorCode.ALREADY_STARTED);
        }
        if (gathering.getStatus() != GatheringStatus.OPEN
                && gathering.getStatus() != GatheringStatus.FULL) {
            throw new GatheringException(GatheringErrorCode.NOT_EDITABLE);
        }
        if (!request.hasChanges()) {
            throw new GatheringException(GatheringErrorCode.EMPTY_UPDATE);
        }
        if (request.startsAt() != null && !request.startsAt().isAfter(now)) {
            throw new GatheringException(GatheringErrorCode.INVALID_START_TIME);
        }

        long participantCount = participantRepository.countByGatheringAndStatus(
                gatheringId, ParticipantStatus.JOINED);
        if (request.capacity() != null && request.capacity() < participantCount) {
            throw new GatheringException(GatheringErrorCode.INVALID_CAPACITY);
        }

        gathering.update(trim(request.title()), trim(request.description()),
                trim(request.concept()), trim(request.meetingPlace()), request.startsAt(),
                request.capacity() == null ? null : request.capacity().shortValue());
        if (gathering.getCapacity() == participantCount) {
            gathering.markFull();
        } else if (gathering.getStatus() == GatheringStatus.FULL) {
            gathering.reopen();
        }

        return new UpdateGatheringResponse(gathering.getId(), gathering.getTitle(),
                gathering.getDescription(), gathering.getConcept(), gathering.getMeetingPlace(),
                gathering.getStartsAt(), gathering.getCapacity(), participantCount,
                gathering.getStatus());
    }

    @Transactional
    public void delete(Long gatheringId, Long userId) {
        Gathering gathering = findForHost(gatheringId, userId);
        gathering.softDelete(OffsetDateTime.now(clock));
    }

    private Gathering findForHost(Long gatheringId, Long userId) {
        Gathering gathering = gatheringRepository.findByIdForUpdate(gatheringId)
                .orElseThrow(() -> new GatheringException(
                        GatheringErrorCode.GATHERING_NOT_FOUND));
        if (!gathering.getCreator().getId().equals(userId)) {
            throw new GatheringException(GatheringErrorCode.HOST_PERMISSION_REQUIRED);
        }
        return gathering;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
