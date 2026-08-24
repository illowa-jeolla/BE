package com.example.travel.domain.gathering.service.cleanup;

import com.example.travel.domain.gathering.repository.GatheringParticipantRepository;
import com.example.travel.domain.gathering.repository.GatheringRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class ExpiredGatheringCleanupService {
    private final GatheringParticipantRepository participantRepository;
    private final GatheringRepository gatheringRepository;

    public ExpiredGatheringCleanupService(GatheringParticipantRepository participantRepository,
                                          GatheringRepository gatheringRepository) {
        this.participantRepository = participantRepository;
        this.gatheringRepository = gatheringRepository;
    }

    @Transactional
    public int deleteExpired(OffsetDateTime cutoff) {
        participantRepository.deleteForExpiredGatherings(cutoff);
        return gatheringRepository.deleteExpired(cutoff);
    }
}
