package com.example.travel.domain.gathering.service;

import com.example.travel.domain.gathering.dto.item.GatheringParticipantItem;
import com.example.travel.domain.gathering.dto.response.GatheringParticipantListResponse;
import com.example.travel.domain.gathering.enums.ParticipantStatus;
import com.example.travel.domain.gathering.exception.GatheringErrorCode;
import com.example.travel.domain.gathering.exception.GatheringException;
import com.example.travel.domain.gathering.repository.GatheringParticipantRepository;
import com.example.travel.domain.gathering.repository.GatheringRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GatheringParticipantQueryService {
    private final GatheringRepository gatheringRepository;
    private final GatheringParticipantRepository participantRepository;

    public GatheringParticipantQueryService(GatheringRepository gatheringRepository,
                                             GatheringParticipantRepository participantRepository) {
        this.gatheringRepository = gatheringRepository;
        this.participantRepository = participantRepository;
    }

    @Transactional(readOnly = true)
    public GatheringParticipantListResponse findParticipants(Long gatheringId, Long userId) {
        if (!gatheringRepository.existsActiveById(gatheringId)) {
            throw new GatheringException(GatheringErrorCode.GATHERING_NOT_FOUND);
        }
        if (!participantRepository.existsJoinedParticipant(
                gatheringId, userId, ParticipantStatus.JOINED)) {
            throw new GatheringException(GatheringErrorCode.PARTICIPANT_LIST_FORBIDDEN);
        }

        List<GatheringParticipantItem> participants = participantRepository
                .findCurrentParticipants(gatheringId, ParticipantStatus.JOINED)
                .stream()
                .map(candidate -> new GatheringParticipantItem(candidate.userId(),
                        candidate.nickname(), candidate.avatarUrl(), candidate.role(),
                        candidate.status(), candidate.joinedAt()))
                .toList();
        return new GatheringParticipantListResponse(
                gatheringId, participants.size(), participants);
    }
}
