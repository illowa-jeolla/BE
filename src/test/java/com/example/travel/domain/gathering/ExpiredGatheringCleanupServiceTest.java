package com.example.travel.domain.gathering;

import com.example.travel.domain.gathering.repository.GatheringParticipantRepository;
import com.example.travel.domain.gathering.repository.GatheringRepository;
import com.example.travel.domain.gathering.service.cleanup.ExpiredGatheringCleanupService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExpiredGatheringCleanupServiceTest {
    @Test
    void deletesParticipantsBeforeExpiredGatherings() {
        GatheringParticipantRepository participantRepository =
                mock(GatheringParticipantRepository.class);
        GatheringRepository gatheringRepository = mock(GatheringRepository.class);
        ExpiredGatheringCleanupService service =
                new ExpiredGatheringCleanupService(participantRepository, gatheringRepository);
        OffsetDateTime cutoff = OffsetDateTime.parse("2026-08-13T12:00:00+09:00");
        when(gatheringRepository.deleteExpired(cutoff)).thenReturn(3);

        int deleted = service.deleteExpired(cutoff);

        assertThat(deleted).isEqualTo(3);
        InOrder inOrder = inOrder(participantRepository, gatheringRepository);
        inOrder.verify(participantRepository).deleteForExpiredGatherings(cutoff);
        inOrder.verify(gatheringRepository).deleteExpired(cutoff);
    }
}
