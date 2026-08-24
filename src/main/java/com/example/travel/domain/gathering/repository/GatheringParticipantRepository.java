package com.example.travel.domain.gathering.repository;

import com.example.travel.domain.gathering.entity.GatheringParticipant;
import com.example.travel.domain.gathering.entity.id.GatheringParticipantId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import com.example.travel.domain.gathering.enums.ParticipantStatus;
import com.example.travel.domain.gathering.repository.projection.GatheringParticipantProjection;

public interface GatheringParticipantRepository
        extends JpaRepository<GatheringParticipant, GatheringParticipantId> {
    @Query("""
            select count(gp) > 0
            from GatheringParticipant gp
            where gp.gathering.id = :gatheringId
              and gp.user.id = :userId
              and gp.status = :status
            """)
    boolean existsJoinedParticipant(@Param("gatheringId") Long gatheringId,
                                    @Param("userId") Long userId,
                                    @Param("status") ParticipantStatus status);

    @Query("""
            select new com.example.travel.domain.gathering.repository.projection.GatheringParticipantProjection(
                gp.user.id, gp.user.nickname, gp.user.avatarUrl,
                gp.participantRole, gp.status, gp.joinedAt
            )
            from GatheringParticipant gp
            where gp.gathering.id = :gatheringId and gp.status = :status
            order by case when gp.participantRole = com.example.travel.domain.gathering.enums.ParticipantRole.HOST
                          then 0 else 1 end,
                     gp.joinedAt asc, gp.user.id asc
            """)
    java.util.List<GatheringParticipantProjection> findCurrentParticipants(
            @Param("gatheringId") Long gatheringId,
            @Param("status") ParticipantStatus status);

    @Query("""
            select gp
            from GatheringParticipant gp
            where gp.gathering.id = :gatheringId and gp.user.id = :userId
            """)
    Optional<GatheringParticipant> findParticipant(
            @Param("gatheringId") Long gatheringId,
            @Param("userId") Long userId);

    @Query("""
            select count(gp)
            from GatheringParticipant gp
            where gp.gathering.id = :gatheringId and gp.status = :status
            """)
    long countByGatheringAndStatus(@Param("gatheringId") Long gatheringId,
                                   @Param("status") ParticipantStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from GatheringParticipant gp where gp.gathering.id in " +
            "(select g.id from Gathering g " +
            "where g.deletedAt is not null or g.startsAt <= :cutoff)")
    int deleteForExpiredGatherings(@Param("cutoff") OffsetDateTime cutoff);
}
