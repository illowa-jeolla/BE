package com.example.travel.domain.gathering.service;

import com.example.travel.domain.gathering.dto.JoinGatheringResponse;
import com.example.travel.domain.gathering.dto.CancelGatheringParticipationResponse;
import com.example.travel.domain.gathering.entity.Gathering;
import com.example.travel.domain.gathering.entity.GatheringParticipant;
import com.example.travel.domain.gathering.enums.GatheringStatus;
import com.example.travel.domain.gathering.enums.ParticipantStatus;
import com.example.travel.domain.gathering.enums.ParticipantRole;
import com.example.travel.domain.gathering.exception.GatheringErrorCode;
import com.example.travel.domain.gathering.exception.GatheringException;
import com.example.travel.domain.gathering.repository.GatheringParticipantRepository;
import com.example.travel.domain.gathering.repository.GatheringRepository;
import com.example.travel.domain.user.entity.User;
import com.example.travel.domain.user.enums.UserStatus;
import com.example.travel.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

@Service
public class GatheringParticipationService {
    private final GatheringRepository gatheringRepository;
    private final GatheringParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public GatheringParticipationService(GatheringRepository gatheringRepository,
                                         GatheringParticipantRepository participantRepository,
                                         UserRepository userRepository,
                                         Clock clock) {
        this.gatheringRepository = gatheringRepository;
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional
    public JoinGatheringResponse join(Long gatheringId, Long userId) {
        Gathering gathering = gatheringRepository.findByIdForUpdate(gatheringId)
                .orElseThrow(() -> new GatheringException(
                        GatheringErrorCode.GATHERING_NOT_FOUND));
        User user = userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new GatheringException(GatheringErrorCode.USER_NOT_FOUND));

        participantRepository.findParticipant(gatheringId, userId)
                .ifPresent(participant -> {
                    GatheringErrorCode errorCode = participant.getStatus()
                            == ParticipantStatus.CANCELLED
                            ? GatheringErrorCode.REJOIN_NOT_ALLOWED
                            : GatheringErrorCode.ALREADY_JOINED;
                    throw new GatheringException(errorCode);
                });
        if (!gathering.getStartsAt().isAfter(OffsetDateTime.now(clock))) {
            throw new GatheringException(GatheringErrorCode.ALREADY_STARTED);
        }
        if (gathering.getStatus() != GatheringStatus.OPEN) {
            throw new GatheringException(GatheringErrorCode.NOT_OPEN);
        }

        long currentCount = participantRepository.countByGatheringAndStatus(
                gatheringId, ParticipantStatus.JOINED);
        if (currentCount >= gathering.getCapacity()) {
            gathering.markFull();
            throw new GatheringException(GatheringErrorCode.CAPACITY_FULL);
        }

        participantRepository.save(GatheringParticipant.createMember(gathering, user));
        long participantCount = currentCount + 1;
        if (participantCount >= gathering.getCapacity()) {
            gathering.markFull();
        }

        return new JoinGatheringResponse(gatheringId, ParticipantStatus.JOINED,
                participantCount, gathering.getStatus());
    }

    @Transactional
    public CancelGatheringParticipationResponse cancel(Long gatheringId, Long userId) {
        Gathering gathering = gatheringRepository.findByIdForUpdate(gatheringId)
                .orElseThrow(() -> new GatheringException(
                        GatheringErrorCode.GATHERING_NOT_FOUND));
        GatheringParticipant participant = participantRepository.findParticipant(
                        gatheringId, userId)
                .orElseThrow(() -> new GatheringException(
                        GatheringErrorCode.PARTICIPANT_NOT_FOUND));

        if (participant.getParticipantRole() == ParticipantRole.HOST) {
            throw new GatheringException(
                    GatheringErrorCode.HOST_CANNOT_CANCEL_PARTICIPATION);
        }
        if (participant.getStatus() != ParticipantStatus.JOINED) {
            throw new GatheringException(GatheringErrorCode.ALREADY_CANCELLED);
        }

        long participantCount = Math.max(0, participantRepository.countByGatheringAndStatus(
                gatheringId, ParticipantStatus.JOINED) - 1);
        participant.cancel();
        if (gathering.getStatus() == GatheringStatus.FULL) {
            gathering.reopen();
        }

        return new CancelGatheringParticipationResponse(gatheringId,
                ParticipantStatus.CANCELLED, participantCount, gathering.getStatus());
    }
}
