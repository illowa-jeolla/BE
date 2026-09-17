package com.example.travel.domain.community.service;

import com.example.travel.domain.community.dto.request.UpdateTravelPostDraftRequest;
import com.example.travel.domain.community.entity.TravelPost;
import com.example.travel.domain.community.enums.TravelPostStatus;
import com.example.travel.domain.community.exception.CommunityException;
import com.example.travel.domain.community.repository.TravelPostRepository;
import com.example.travel.domain.community.repository.TravelPostImageRepository;
import com.example.travel.domain.community.storage.ImageStorage;
import com.example.travel.domain.community.service.cleanup.S3ObjectDeletionQueue;
import com.example.travel.domain.region.entity.Region;
import com.example.travel.domain.region.repository.RegionRepository;
import com.example.travel.domain.user.entity.User;
import com.example.travel.domain.user.enums.UserStatus;
import com.example.travel.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TravelPostDraftServiceTest {
    private UserRepository userRepository;
    private TravelPostRepository travelPostRepository;
    private RegionRepository regionRepository;
    private TravelPostImageRepository imageRepository;
    private ImageStorage imageStorage;
    private TravelPostImageOrderService imageOrderService;
    private S3ObjectDeletionQueue deletionQueue;
    private TravelPostDraftService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        travelPostRepository = mock(TravelPostRepository.class);
        regionRepository = mock(RegionRepository.class);
        imageRepository = mock(TravelPostImageRepository.class);
        imageStorage = mock(ImageStorage.class);
        imageOrderService = mock(TravelPostImageOrderService.class);
        deletionQueue = mock(S3ObjectDeletionQueue.class);
        service = new TravelPostDraftService(userRepository, travelPostRepository,
                regionRepository, imageRepository, imageStorage, imageOrderService, deletionQueue);
    }

    @Test
    void createsEmptyDraftForActiveUser() {
        User author = mock(User.class);
        when(userRepository.findByIdAndStatusForUpdate(7L, UserStatus.ACTIVE))
                .thenReturn(Optional.of(author));
        when(travelPostRepository.findFirstByAuthorIdAndStatusOrderByIdDesc(
                7L, TravelPostStatus.DRAFT)).thenReturn(Optional.empty());
        when(travelPostRepository.save(org.mockito.ArgumentMatchers.any(TravelPost.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.open(7L);

        ArgumentCaptor<TravelPost> captor = ArgumentCaptor.forClass(TravelPost.class);
        verify(travelPostRepository).save(captor.capture());
        TravelPost draft = captor.getValue();
        assertThat(draft.getAuthor()).isSameAs(author);
        assertThat(draft.getStatus()).isEqualTo(TravelPostStatus.DRAFT);
        assertThat(draft.getRegion()).isNull();
        assertThat(draft.getTitle()).isNull();
        assertThat(draft.getContent()).isNull();
        assertThat(response.status()).isEqualTo(TravelPostStatus.DRAFT);
        assertThat(response.resumed()).isFalse();
    }

    @Test
    void resumesExistingDraftWithoutCreatingAnotherOne() {
        User author = mock(User.class);
        TravelPost existingDraft = mock(TravelPost.class);
        when(userRepository.findByIdAndStatusForUpdate(7L, UserStatus.ACTIVE))
                .thenReturn(Optional.of(author));
        when(travelPostRepository.findFirstByAuthorIdAndStatusOrderByIdDesc(
                7L, TravelPostStatus.DRAFT)).thenReturn(Optional.of(existingDraft));
        when(existingDraft.getId()).thenReturn(15L);
        when(existingDraft.getStatus()).thenReturn(TravelPostStatus.DRAFT);

        var response = service.open(7L);

        assertThat(response.draftId()).isEqualTo(15L);
        assertThat(response.status()).isEqualTo(TravelPostStatus.DRAFT);
        assertThat(response.resumed()).isTrue();
        verify(travelPostRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsUnknownOrInactiveUser() {
        when(userRepository.findByIdAndStatusForUpdate(7L, UserStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.open(7L))
                .isInstanceOfSatisfying(CommunityException.class, exception ->
                        assertThat(exception.getCode())
                                .isEqualTo("COMMUNITY_404_USER_NOT_FOUND"));
        verify(travelPostRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void savesCurrentDraftContentsAndActiveRegion() {
        User author = mock(User.class);
        Region region = mock(Region.class);
        TravelPost draft = TravelPost.createDraft(author);
        when(travelPostRepository.findOwnedByIdAndStatusForUpdate(
                15L, 7L, TravelPostStatus.DRAFT)).thenReturn(Optional.of(draft));
        when(regionRepository.findActiveById(3L)).thenReturn(Optional.of(region));
        when(region.getId()).thenReturn(3L);
        when(region.getName()).thenReturn("여수");

        var response = service.update(7L, 15L,
                new UpdateTravelPostDraftRequest(3L, " 여수 여행 ", " 힐링 ",
                        " 바다를 보러 갑니다. ", null));

        assertThat(draft.getRegion()).isSameAs(region);
        assertThat(draft.getTitle()).isEqualTo("여수 여행");
        assertThat(draft.getConcept()).isEqualTo("힐링");
        assertThat(draft.getContent()).isEqualTo("바다를 보러 갑니다.");
        assertThat(response.regionId()).isEqualTo(3L);
        assertThat(response.regionName()).isEqualTo("여수");
        verify(travelPostRepository).flush();
    }

    @Test
    void clearsBlankDraftFieldsAndRegion() {
        TravelPost draft = TravelPost.createDraft(mock(User.class));
        when(travelPostRepository.findOwnedByIdAndStatusForUpdate(
                15L, 7L, TravelPostStatus.DRAFT)).thenReturn(Optional.of(draft));

        var response = service.update(7L, 15L,
                new UpdateTravelPostDraftRequest(null, "  ", null, "\n", null));

        assertThat(response.regionId()).isNull();
        assertThat(response.title()).isNull();
        assertThat(response.concept()).isNull();
        assertThat(response.content()).isNull();
        verify(regionRepository, never()).findActiveById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsDraftOwnedByAnotherUserOrNotInDraftState() {
        when(travelPostRepository.findOwnedByIdAndStatusForUpdate(
                15L, 7L, TravelPostStatus.DRAFT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(7L, 15L,
                new UpdateTravelPostDraftRequest(null, null, null, null, null)))
                .isInstanceOfSatisfying(CommunityException.class, exception ->
                        assertThat(exception.getCode())
                                .isEqualTo("COMMUNITY_404_DRAFT_NOT_FOUND"));
    }

    @Test
    void rejectsInactiveRegion() {
        TravelPost draft = TravelPost.createDraft(mock(User.class));
        when(travelPostRepository.findOwnedByIdAndStatusForUpdate(
                15L, 7L, TravelPostStatus.DRAFT)).thenReturn(Optional.of(draft));
        when(regionRepository.findActiveById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(7L, 15L,
                new UpdateTravelPostDraftRequest(3L, null, null, null, null)))
                .isInstanceOfSatisfying(CommunityException.class, exception ->
                        assertThat(exception.getCode())
                                .isEqualTo("COMMUNITY_404_REGION_NOT_FOUND"));
    }

    @Test
    void publishesCompleteDraftWithoutCopyingPost() {
        User author = mock(User.class);
        Region region = mock(Region.class);
        when(author.getId()).thenReturn(7L);
        when(region.getId()).thenReturn(3L);
        when(region.getName()).thenReturn("여수");
        TravelPost draft = TravelPost.createDraft(author);
        draft.updateDraft(region, "여수 여행", "힐링", "바다 여행기");
        when(travelPostRepository.findOwnedByIdAndStatusForUpdate(
                15L, 7L, TravelPostStatus.DRAFT)).thenReturn(Optional.of(draft));

        var response = service.publish(7L, 15L);

        assertThat(draft.getStatus()).isEqualTo(TravelPostStatus.PUBLISHED);
        assertThat(response.title()).isEqualTo("여수 여행");
        verify(travelPostRepository).flush();
    }

    @Test
    void rejectsPublishingIncompleteDraft() {
        TravelPost draft = TravelPost.createDraft(mock(User.class));
        when(travelPostRepository.findOwnedByIdAndStatusForUpdate(
                15L, 7L, TravelPostStatus.DRAFT)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.publish(7L, 15L))
                .isInstanceOfSatisfying(CommunityException.class, exception ->
                        assertThat(exception.getCode())
                                .isEqualTo("COMMUNITY_400_PUBLISH_TITLE_REQUIRED"));
    }
}
