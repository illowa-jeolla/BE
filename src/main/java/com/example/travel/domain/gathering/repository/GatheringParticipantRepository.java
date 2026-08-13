package com.example.travel.domain.gathering.repository;

import com.example.travel.domain.gathering.entity.GatheringParticipant;
import com.example.travel.domain.gathering.entity.id.GatheringParticipantId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;

public interface GatheringParticipantRepository
        extends JpaRepository<GatheringParticipant, GatheringParticipantId> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from GatheringParticipant gp where gp.gathering.id in " +
            "(select g.id from Gathering g where g.startsAt <= :cutoff)")
    int deleteForExpiredGatherings(@Param("cutoff") OffsetDateTime cutoff);
}
