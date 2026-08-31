package com.example.travel.domain.gathering.repository;

import com.example.travel.domain.gathering.entity.Gathering;
import com.example.travel.domain.gathering.repository.projection.GatheringSearchProjection;
import com.example.travel.domain.gathering.repository.projection.GatheringDetailProjection;
import com.example.travel.domain.gathering.repository.projection.MyGatheringProjection;
import com.example.travel.domain.gathering.enums.GatheringStatus;
import com.example.travel.domain.gathering.enums.ParticipantRole;
import com.example.travel.domain.gathering.enums.ParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jakarta.persistence.LockModeType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface GatheringRepository extends JpaRepository<Gathering, Long> {
    @Query(value = """
            select new com.example.travel.domain.gathering.repository.projection.MyGatheringProjection(
                g.id, g.title, g.region.id, g.region.name, g.concept, g.meetingPlace,
                g.startsAt, g.capacity, g.status,
                count(case when gp.status = :joinedStatus then 1 else null end)
            )
            from Gathering g
            left join GatheringParticipant gp on gp.gathering = g
            where g.creator.id = :userId
              and g.deletedAt is null
            group by g.id, g.title, g.region.id, g.region.name, g.concept,
                     g.meetingPlace, g.startsAt, g.capacity, g.status
            """,
            countQuery = """
                    select count(g) from Gathering g
                    where g.creator.id = :userId and g.deletedAt is null
                    """)
    Page<MyGatheringProjection> findHostedGatherings(
            @Param("userId") Long userId,
            @Param("joinedStatus") ParticipantStatus joinedStatus,
            Pageable pageable);

    @Query(value = """
            select new com.example.travel.domain.gathering.repository.projection.MyGatheringProjection(
                g.id, g.title, g.region.id, g.region.name, g.concept, g.meetingPlace,
                g.startsAt, g.capacity, g.status,
                count(case when gp.status = :joinedStatus then 1 else null end)
            )
            from Gathering g
            left join GatheringParticipant gp on gp.gathering = g
            where g.deletedAt is null
              and exists (
                select mine.id
                from GatheringParticipant mine
                where mine.gathering = g
                  and mine.user.id = :userId
                  and mine.participantRole = :memberRole
                  and mine.status = :joinedStatus
            )
            group by g.id, g.title, g.region.id, g.region.name, g.concept,
                     g.meetingPlace, g.startsAt, g.capacity, g.status
            """,
            countQuery = """
                    select count(g)
                    from Gathering g
                    where g.deletedAt is null
                      and exists (
                        select mine.id
                        from GatheringParticipant mine
                        where mine.gathering = g
                          and mine.user.id = :userId
                          and mine.participantRole = :memberRole
                          and mine.status = :joinedStatus
                    )
                    """)
    Page<MyGatheringProjection> findJoinedGatherings(
            @Param("userId") Long userId,
            @Param("memberRole") ParticipantRole memberRole,
            @Param("joinedStatus") ParticipantStatus joinedStatus,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select g from Gathering g
            where g.id = :gatheringId and g.deletedAt is null
            """)
    Optional<Gathering> findByIdForUpdate(@Param("gatheringId") Long gatheringId);

    @Query("""
            select new com.example.travel.domain.gathering.repository.projection.GatheringDetailProjection(
                g.id, g.title, g.description, g.region.id, g.region.name,
                g.concept, g.meetingPlace, g.latitude, g.longitude, g.startsAt,
                g.capacity, g.status, g.creator.id, g.creator.nickname,
                g.creator.avatarUrl, g.createdAt, g.updatedAt,
                count(case when gp.status = :participantStatus then 1 else null end),
                count(case when gp.status = :participantStatus and gp.user.id = :userId
                           then 1 else null end)
            )
            from Gathering g
            left join GatheringParticipant gp on gp.gathering = g
            where g.id = :gatheringId
              and g.deletedAt is null
            group by g.id, g.title, g.description, g.region.id, g.region.name,
                     g.concept, g.meetingPlace, g.latitude, g.longitude, g.startsAt,
                     g.capacity, g.status, g.creator.id, g.creator.nickname,
                     g.creator.avatarUrl, g.createdAt, g.updatedAt
            """)
    Optional<GatheringDetailProjection> findDetail(
            @Param("gatheringId") Long gatheringId,
            @Param("userId") Long userId,
            @Param("participantStatus") ParticipantStatus participantStatus);

    @Query("""
            select new com.example.travel.domain.gathering.repository.projection.GatheringSearchProjection(
                g.id, g.title, g.region.id, g.region.name, g.concept, g.meetingPlace,
                g.startsAt, g.capacity, g.status, g.creator.id, g.creator.nickname,
                count(case when gp.status = :participantStatus then 1 else null end),
                count(case when gp.status = :participantStatus and gp.user.id = :userId
                           then 1 else null end)
            )
            from Gathering g
            left join GatheringParticipant gp on gp.gathering = g
            where g.deletedAt is null
              and g.status = :status
              and (:region = '' or g.region.name = :region)
              and g.region.active = true
              and g.startsAt >= :startsAt
              and g.startsAt < :endsAt
            group by g.id, g.title, g.region.id, g.region.name, g.concept, g.meetingPlace,
                     g.startsAt, g.capacity, g.status, g.creator.id, g.creator.nickname
            """)
    List<GatheringSearchProjection> findSearchCandidates(
            @Param("userId") Long userId,
            @Param("region") String region,
            @Param("startsAt") OffsetDateTime startsAt,
            @Param("endsAt") OffsetDateTime endsAt,
            @Param("status") GatheringStatus status,
            @Param("participantStatus") ParticipantStatus participantStatus,
            Pageable pageable);

    @Query("""
            select count(g.id)
            from Gathering g
            where g.deletedAt is null
              and g.status = :status
              and (:region = '' or g.region.name = :region)
              and g.region.active = true
              and g.startsAt >= :startsAt
              and g.startsAt < :endsAt
            """)
    long countSearchCandidates(@Param("region") String region,
                               @Param("startsAt") OffsetDateTime startsAt,
                               @Param("endsAt") OffsetDateTime endsAt,
                               @Param("status") GatheringStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from Gathering g
            where g.deletedAt is not null or g.startsAt <= :cutoff
            """)
    int deleteExpired(@Param("cutoff") OffsetDateTime cutoff);

    @Query("""
            select count(g) > 0 from Gathering g
            where g.id = :gatheringId and g.deletedAt is null
            """)
    boolean existsActiveById(@Param("gatheringId") Long gatheringId);
}
