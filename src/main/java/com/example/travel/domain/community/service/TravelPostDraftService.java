package com.example.travel.domain.community.service;

import com.example.travel.domain.community.dto.request.UpdateTravelPostDraftRequest;
import com.example.travel.domain.community.dto.response.CreateTravelPostDraftResponse;
import com.example.travel.domain.community.dto.response.TravelPostDraftResponse;
import com.example.travel.domain.community.entity.TravelPost;
import com.example.travel.domain.community.enums.TravelPostStatus;
import com.example.travel.domain.community.exception.CommunityErrorCode;
import com.example.travel.domain.community.exception.CommunityException;
import com.example.travel.domain.community.repository.TravelPostRepository;
import com.example.travel.domain.community.repository.TravelPostImageRepository;
import com.example.travel.domain.community.dto.response.DraftImageItem;
import com.example.travel.domain.community.dto.response.TravelPostDetailResponse;
import com.example.travel.domain.community.storage.ImageStorage;
import com.example.travel.domain.community.service.cleanup.S3ObjectDeletionQueue;
import com.example.travel.domain.region.entity.Region;
import com.example.travel.domain.region.repository.RegionRepository;
import com.example.travel.domain.user.entity.User;
import com.example.travel.domain.user.enums.UserStatus;
import com.example.travel.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TravelPostDraftService {
    private final UserRepository userRepository;
    private final TravelPostRepository travelPostRepository;
    private final RegionRepository regionRepository;
    private final TravelPostImageRepository imageRepository;
    private final ImageStorage imageStorage;
    private final TravelPostImageOrderService imageOrderService;
    private final S3ObjectDeletionQueue deletionQueue;

    public TravelPostDraftService(UserRepository userRepository,
                                  TravelPostRepository travelPostRepository,
                                  RegionRepository regionRepository,
                                  TravelPostImageRepository imageRepository,
                                  ImageStorage imageStorage,
                                  TravelPostImageOrderService imageOrderService,
                                  S3ObjectDeletionQueue deletionQueue) {
        this.userRepository = userRepository;
        this.travelPostRepository = travelPostRepository;
        this.regionRepository = regionRepository;
        this.imageRepository = imageRepository;
        this.imageStorage = imageStorage;
        this.imageOrderService = imageOrderService;
        this.deletionQueue = deletionQueue;
    }

    @Transactional
    public CreateTravelPostDraftResponse open(Long userId) {
        User author = userRepository.findByIdAndStatusForUpdate(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.USER_NOT_FOUND));
        var existingDraft = travelPostRepository
                .findFirstByAuthorIdAndStatusOrderByIdDesc(userId, TravelPostStatus.DRAFT);
        if (existingDraft.isPresent()) {
            TravelPost draft = existingDraft.get();
            return response(draft, true);
        }

        TravelPost draft = travelPostRepository.save(TravelPost.createDraft(author));
        return response(draft, false);
    }

    @Transactional
    public TravelPostDraftResponse update(Long userId, Long draftId,
                                          UpdateTravelPostDraftRequest request) {
        TravelPost draft = travelPostRepository.findOwnedByIdAndStatusForUpdate(
                        draftId, userId, TravelPostStatus.DRAFT)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.DRAFT_NOT_FOUND));
        Region region = request.regionId() == null ? null
                : regionRepository.findActiveById(request.regionId())
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.REGION_NOT_FOUND));
        draft.updateDraft(region, normalize(request.title()), normalize(request.concept()),
                normalize(request.content()));
        imageOrderService.reorder(draftId, request.imageIds());
        travelPostRepository.flush();
        return TravelPostDraftResponse.from(draft, images(draft.getId()));
    }

    @Transactional
    public TravelPostDraftResponse findDetail(Long userId, Long draftId) {
        TravelPost draft = travelPostRepository.findOwnedByIdAndStatusForUpdate(
                        draftId, userId, TravelPostStatus.DRAFT)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.DRAFT_NOT_FOUND));
        return TravelPostDraftResponse.from(draft, images(draftId));
    }

    @Transactional
    public void delete(Long userId, Long draftId) {
        TravelPost draft = travelPostRepository.findOwnedByIdAndStatusForUpdate(
                        draftId, userId, TravelPostStatus.DRAFT)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.DRAFT_NOT_FOUND));
        List<String> objectKeys = imageRepository.findAllByPostIdOrderByDisplayOrderAsc(draftId)
                .stream().map(image -> image.getObjectKey()).toList();
        imageRepository.deleteAllByPostId(draftId);
        travelPostRepository.delete(draft);
        objectKeys.forEach(deletionQueue::schedule);
        travelPostRepository.flush();
    }

    @Transactional
    public TravelPostDetailResponse publish(Long userId, Long draftId) {
        TravelPost draft = travelPostRepository.findOwnedByIdAndStatusForUpdate(
                        draftId, userId, TravelPostStatus.DRAFT)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.DRAFT_NOT_FOUND));
        if (draft.getTitle() == null || draft.getTitle().isBlank()) {
            throw new CommunityException(CommunityErrorCode.PUBLISH_TITLE_REQUIRED);
        }
        if (draft.getContent() == null || draft.getContent().isBlank()) {
            throw new CommunityException(CommunityErrorCode.PUBLISH_CONTENT_REQUIRED);
        }
        if (draft.getRegion() == null) {
            throw new CommunityException(CommunityErrorCode.PUBLISH_REGION_REQUIRED);
        }
        draft.publish();
        travelPostRepository.flush();
        List<TravelPostDetailResponse.PostImageItem> postImages = imageRepository
                .findAllByPostIdOrderByDisplayOrderAsc(draftId).stream()
                .map(image -> new TravelPostDetailResponse.PostImageItem(image.getId(),
                        imageStorage.accessUrl(image.getObjectKey()),
                        image.getAltText(), image.getDisplayOrder())).toList();
        return TravelPostDetailResponse.from(draft, userId, 0, postImages);
    }

    private CreateTravelPostDraftResponse response(TravelPost draft, boolean resumed) {
        return new CreateTravelPostDraftResponse(
                draft.getId(),
                draft.getStatus(),
                draft.getRegion() == null ? null : draft.getRegion().getId(),
                draft.getRegion() == null ? null : draft.getRegion().getName(),
                draft.getTitle(),
                draft.getConcept(),
                draft.getContent(),
                draft.getUpdatedAt(),
                images(draft.getId()),
                resumed);
    }

    private List<DraftImageItem> images(Long draftId) {
        if (draftId == null) {
            return List.of();
        }
        return imageRepository.findAllByPostIdOrderByDisplayOrderAsc(draftId).stream()
                .map(image -> DraftImageItem.from(image,
                        imageStorage.accessUrl(image.getObjectKey()))).toList();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
