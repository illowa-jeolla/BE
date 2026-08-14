package com.example.travel.domain.gathering.entity;

import com.example.travel.domain.gathering.entity.id.GatheringParticipantId;
import com.example.travel.domain.gathering.enums.ParticipantRole;
import com.example.travel.domain.gathering.enums.ParticipantStatus;
import com.example.travel.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;

@Getter
@Entity
@Table(name = "gathering_participants")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GatheringParticipant {
    @EmbeddedId
    private GatheringParticipantId id;

    @MapsId("gatheringId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gathering_id", nullable = false)
    private Gathering gathering;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "participant_role", nullable = false, length = 20)
    private ParticipantRole participantRole = ParticipantRole.MEMBER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ParticipantStatus status = ParticipantStatus.JOINED;

    @Column(name = "joined_at", nullable = false, insertable = false, updatable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private OffsetDateTime joinedAt;

    private GatheringParticipant(Gathering gathering, User user, ParticipantRole participantRole) {
        this.id = new GatheringParticipantId(gathering.getId(), user.getId());
        this.gathering = gathering;
        this.user = user;
        this.participantRole = participantRole;
        this.status = ParticipantStatus.JOINED;
    }

    public static GatheringParticipant createHost(Gathering gathering, User user) {
        return new GatheringParticipant(gathering, user, ParticipantRole.HOST);
    }

    public static GatheringParticipant createMember(Gathering gathering, User user) {
        return new GatheringParticipant(gathering, user, ParticipantRole.MEMBER);
    }

    public void cancel() {
        this.status = ParticipantStatus.CANCELLED;
    }
}
