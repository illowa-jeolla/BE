package com.example.travel.domain.gathering;

import com.example.travel.domain.gathering.dto.CreateGatheringRequest;
import com.example.travel.domain.gathering.dto.CreateGatheringResponse;
import com.example.travel.domain.gathering.dto.UpdateGatheringRequest;
import com.example.travel.domain.gathering.entity.Gathering;
import com.example.travel.domain.gathering.entity.GatheringParticipant;
import com.example.travel.domain.gathering.enums.GatheringStatus;
import com.example.travel.domain.gathering.enums.ParticipantStatus;
import com.example.travel.domain.gathering.exception.GatheringException;
import com.example.travel.domain.gathering.repository.GatheringParticipantRepository;
import com.example.travel.domain.gathering.repository.GatheringRepository;
import com.example.travel.domain.gathering.service.GatheringService;
import com.example.travel.domain.region.entity.Region;
import com.example.travel.domain.region.repository.RegionRepository;
import com.example.travel.domain.user.entity.User;
import com.example.travel.domain.user.enums.UserStatus;
import com.example.travel.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GatheringServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-13T03:00:00Z");

    private UserRepository userRepository;
    private RegionRepository regionRepository;
    private GatheringRepository gatheringRepository;
    private GatheringParticipantRepository participantRepository;
    private GatheringService gatheringService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        regionRepository = mock(RegionRepository.class);
        gatheringRepository = mock(GatheringRepository.class);
        participantRepository = mock(GatheringParticipantRepository.class);
        gatheringService = new GatheringService(userRepository, regionRepository,
                gatheringRepository, participantRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsGatheringAndRegistersCreatorAsHost() {
        User creator = mock(User.class);
        Region region = mock(Region.class);
        when(creator.getId()).thenReturn(7L);
        when(userRepository.findByIdAndStatus(7L, UserStatus.ACTIVE))
                .thenReturn(Optional.of(creator));
        when(regionRepository.findActiveByName("여수")).thenReturn(Optional.of(region));
        when(gatheringRepository.save(org.mockito.ArgumentMatchers.any(Gathering.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateGatheringResponse response = gatheringService.create(7L, requestAt(
                OffsetDateTime.ofInstant(NOW.plusSeconds(3600), ZoneOffset.UTC)));

        ArgumentCaptor<Gathering> gatheringCaptor = ArgumentCaptor.forClass(Gathering.class);
        verify(gatheringRepository).save(gatheringCaptor.capture());
        Gathering gathering = gatheringCaptor.getValue();
        assertThat(gathering.getCreator()).isSameAs(creator);
        assertThat(gathering.getRegion()).isSameAs(region);
        assertThat(gathering.getTitle()).isEqualTo("여수 밤바다 펍투어");
        assertThat(gathering.getCapacity()).isEqualTo((short) 4);
        assertThat(response.status()).isEqualTo(gathering.getStatus());

        ArgumentCaptor<GatheringParticipant> participantCaptor =
                ArgumentCaptor.forClass(GatheringParticipant.class);
        verify(participantRepository).save(participantCaptor.capture());
        assertThat(participantCaptor.getValue().getGathering()).isSameAs(gathering);
        assertThat(participantCaptor.getValue().getUser()).isSameAs(creator);
        assertThat(participantCaptor.getValue().getParticipantRole().name()).isEqualTo("HOST");
        assertThat(participantCaptor.getValue().getStatus().name()).isEqualTo("JOINED");
    }

    @Test
    void rejectsStartTimeThatIsNoLongerInFuture() {
        User creator = mock(User.class);
        Region region = mock(Region.class);
        when(userRepository.findByIdAndStatus(7L, UserStatus.ACTIVE))
                .thenReturn(Optional.of(creator));
        when(regionRepository.findActiveByName("여수")).thenReturn(Optional.of(region));

        assertThatThrownBy(() -> gatheringService.create(7L,
                requestAt(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC))))
                .isInstanceOfSatisfying(GatheringException.class, exception ->
                        assertThat(exception.getCode())
                                .isEqualTo("GATHERING_400_INVALID_START_TIME"));
        verify(gatheringRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(participantRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsInactiveOrUnknownRegion() {
        User creator = mock(User.class);
        when(userRepository.findByIdAndStatus(7L, UserStatus.ACTIVE))
                .thenReturn(Optional.of(creator));
        when(regionRepository.findActiveByName("여수")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gatheringService.create(7L, requestAt(
                OffsetDateTime.ofInstant(NOW.plusSeconds(3600), ZoneOffset.UTC))))
                .isInstanceOfSatisfying(GatheringException.class, exception ->
                        assertThat(exception.getCode())
                                .isEqualTo("GATHERING_404_REGION_NOT_FOUND"));
    }

    @Test
    void updatesGatheringAsHost() {
        User creator = mock(User.class);
        Gathering gathering = mock(Gathering.class);
        when(creator.getId()).thenReturn(7L);
        when(gathering.getCreator()).thenReturn(creator);
        when(gathering.getStartsAt()).thenReturn(
                OffsetDateTime.ofInstant(NOW.plusSeconds(3600), ZoneOffset.UTC));
        when(gathering.getStatus()).thenReturn(GatheringStatus.OPEN);
        when(gathering.getCapacity()).thenReturn((short) 3);
        when(gathering.getId()).thenReturn(1L);
        when(gatheringRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(gathering));
        when(participantRepository.countByGatheringAndStatus(
                1L, ParticipantStatus.JOINED)).thenReturn(2L);

        gatheringService.update(1L, 7L, new UpdateGatheringRequest(
                " 새 제목 ", null, " 새 콘셉트 ", null, null, 3));

        verify(gathering).update("새 제목", null, "새 콘셉트", null, null, (short) 3);
        verify(gathering, never()).markFull();
    }

    @Test
    void rejectsCapacitySmallerThanCurrentParticipants() {
        User creator = mock(User.class);
        Gathering gathering = mock(Gathering.class);
        when(creator.getId()).thenReturn(7L);
        when(gathering.getCreator()).thenReturn(creator);
        when(gathering.getStartsAt()).thenReturn(
                OffsetDateTime.ofInstant(NOW.plusSeconds(3600), ZoneOffset.UTC));
        when(gathering.getStatus()).thenReturn(GatheringStatus.OPEN);
        when(gatheringRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(gathering));
        when(participantRepository.countByGatheringAndStatus(
                1L, ParticipantStatus.JOINED)).thenReturn(3L);

        assertThatThrownBy(() -> gatheringService.update(1L, 7L,
                new UpdateGatheringRequest(null, null, null, null, null, 2)))
                .isInstanceOfSatisfying(GatheringException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo("GATHERING_400_INVALID_CAPACITY"));
        verify(gathering, never()).update(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsUpdateByNonHost() {
        User creator = mock(User.class);
        Gathering gathering = mock(Gathering.class);
        when(creator.getId()).thenReturn(8L);
        when(gathering.getCreator()).thenReturn(creator);
        when(gatheringRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(gathering));

        assertThatThrownBy(() -> gatheringService.update(1L, 7L,
                new UpdateGatheringRequest("수정", null, null, null, null, null)))
                .isInstanceOfSatisfying(GatheringException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo("GATHERING_403_HOST_PERMISSION_REQUIRED"));
    }

    @Test
    void softDeletesGatheringAsHostWithoutRemovingParticipants() {
        User creator = mock(User.class);
        Gathering gathering = mock(Gathering.class);
        when(creator.getId()).thenReturn(7L);
        when(gathering.getCreator()).thenReturn(creator);
        when(gatheringRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(gathering));

        gatheringService.delete(1L, 7L);

        verify(gathering).softDelete(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
        verify(gatheringRepository, never()).delete(gathering);
    }

    @Test
    void rejectsDeleteByNonHost() {
        User creator = mock(User.class);
        Gathering gathering = mock(Gathering.class);
        when(creator.getId()).thenReturn(8L);
        when(gathering.getCreator()).thenReturn(creator);
        when(gatheringRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(gathering));

        assertThatThrownBy(() -> gatheringService.delete(1L, 7L))
                .isInstanceOfSatisfying(GatheringException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo("GATHERING_403_HOST_PERMISSION_REQUIRED"));
        verify(gatheringRepository, never()).delete(gathering);
    }

    private CreateGatheringRequest requestAt(OffsetDateTime startsAt) {
        return new CreateGatheringRequest(" 여수 ", " 여수 밤바다 펍투어 ", 4,
                " 여수 낭만포차 입구 ", startsAt, " 펍투어 ",
                " 여수 밤바다를 보며 함께 걸어요. ");
    }
}
