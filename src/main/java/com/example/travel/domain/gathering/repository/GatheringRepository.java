package com.example.travel.domain.gathering.repository;

import com.example.travel.domain.gathering.entity.Gathering;
import com.example.travel.domain.gathering.dto.GatheringSearchCandidate;
import com.example.travel.domain.gathering.enums.GatheringStatus;
import com.example.travel.domain.gathering.enums.ParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface GatheringRepository extends JpaRepository<Gathering, Long> {
    @Query("""
            select new com.example.travel.domain.gathering.dto.GatheringSearchCandidate(
                g.id, g.title, g.region.id, g.region.name, g.concept, g.meetingPlace,
                g.startsAt, g.capacity, g.status, g.creator.id, g.creator.nickname,
                count(case when gp.status = :participantStatus then 1 else null end),
                count(case when gp.status = :participantStatus and gp.user.id = :userId
                           then 1 else null end)
            )
            from Gathering g
            left join GatheringParticipant gp on gp.gathering = g
            where g.creator.id <> :userId
              and g.status = :status
              and g.region.name = :region
              and g.region.active = true
              and g.startsAt >= :startsAt
              and g.startsAt < :endsAt
            group by g.id, g.title, g.region.id, g.region.name, g.concept, g.meetingPlace,
                     g.startsAt, g.capacity, g.status, g.creator.id, g.creator.nickname
            """)
    List<GatheringSearchCandidate> findSearchCandidates(
            @Param("userId") Long userId,
            @Param("region") String region,
            @Param("startsAt") OffsetDateTime startsAt,
            @Param("endsAt") OffsetDateTime endsAt,
            @Param("status") GatheringStatus status,
            @Param("participantStatus") ParticipantStatus participantStatus);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Gathering g where g.startsAt <= :cutoff")
    int deleteExpired(@Param("cutoff") OffsetDateTime cutoff);
}
