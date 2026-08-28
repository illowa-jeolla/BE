package com.example.travel.domain.community.service;

import com.example.travel.domain.community.config.CommunityImageProperties;
import com.example.travel.domain.community.dto.response.TravelPostImageResponse;
import com.example.travel.domain.community.dto.response.DraftImageItem;
import com.example.travel.domain.community.dto.response.TravelPostDetailResponse;
import com.example.travel.domain.community.dto.request.ReorderTravelPostImagesRequest;
import com.example.travel.domain.community.entity.TravelPost;
import com.example.travel.domain.community.entity.TravelPostImage;
import com.example.travel.domain.community.enums.TravelPostStatus;
import com.example.travel.domain.community.exception.CommunityErrorCode;
import com.example.travel.domain.community.exception.CommunityException;
import com.example.travel.domain.community.repository.TravelPostImageRepository;
import com.example.travel.domain.community.repository.TravelPostRepository;
import com.example.travel.domain.community.storage.ImageFileValidator;
import com.example.travel.domain.community.storage.ImageStorage;
import com.example.travel.domain.community.service.cleanup.S3ObjectDeletionQueue;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.HashSet;
import java.util.List;

@Service
public class TravelPostImageService {
    private final TravelPostRepository travelPostRepository;
    private final TravelPostImageRepository imageRepository;
    private final ImageFileValidator imageFileValidator;
    private final ImageStorage imageStorage;
    private final int maxImagesPerPost;
    private final S3ObjectDeletionQueue deletionQueue;

    public TravelPostImageService(TravelPostRepository travelPostRepository,
                                  TravelPostImageRepository imageRepository,
                                  ImageFileValidator imageFileValidator,
                                  ImageStorage imageStorage,
                                  CommunityImageProperties properties,
                                  S3ObjectDeletionQueue deletionQueue) {
        this.travelPostRepository = travelPostRepository;
        this.imageRepository = imageRepository;
        this.imageFileValidator = imageFileValidator;
        this.imageStorage = imageStorage;
        this.maxImagesPerPost = properties.maxImagesPerPost();
        this.deletionQueue = deletionQueue;
    }

    @Transactional
    public TravelPostImageResponse upload(Long userId, Long draftId, MultipartFile file) {
        TravelPost draft = travelPostRepository.findOwnedByIdAndStatusForUpdate(
                        draftId, userId, TravelPostStatus.DRAFT)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.DRAFT_NOT_FOUND));
        long imageCount = imageRepository.countByPostId(draftId);
        if (imageCount >= maxImagesPerPost) {
            throw new CommunityException(CommunityErrorCode.IMAGE_LIMIT_EXCEEDED);
        }

        ImageFileValidator.ValidatedImage image = imageFileValidator.validate(file);
        String objectKey = "community/posts/" + draftId + "/" + UUID.randomUUID()
                + "." + image.extension();
        imageStorage.store(objectKey, image.content(), image.contentType());
        deleteStoredFileOnRollback(objectKey);

        TravelPostImage saved = imageRepository.saveAndFlush(TravelPostImage.create(
                draft, objectKey, (short) imageCount));
        draft.touchDraft();
        travelPostRepository.flush();
        return new TravelPostImageResponse(saved.getId(), saved.getObjectKey(),
                imageStorage.accessUrl(saved.getObjectKey()),
                image.contentType(), saved.getDisplayOrder());
    }

    @Transactional
    public TravelPostImageResponse uploadPublished(Long userId, Long postId, MultipartFile file) {
        TravelPost post = ownedPublished(userId, postId);
        long imageCount = imageRepository.countByPostId(postId);
        if (imageCount >= maxImagesPerPost) {
            throw new CommunityException(CommunityErrorCode.IMAGE_LIMIT_EXCEEDED);
        }
        ImageFileValidator.ValidatedImage image = imageFileValidator.validate(file);
        String objectKey = "community/posts/" + postId + "/" + UUID.randomUUID()
                + "." + image.extension();
        imageStorage.store(objectKey, image.content(), image.contentType());
        deleteStoredFileOnRollback(objectKey);
        TravelPostImage saved = imageRepository.saveAndFlush(TravelPostImage.create(
                post, objectKey, (short) imageCount));
        post.touchPublished();
        travelPostRepository.flush();
        return new TravelPostImageResponse(saved.getId(), saved.getObjectKey(),
                imageStorage.accessUrl(saved.getObjectKey()),
                image.contentType(), saved.getDisplayOrder());
    }

    @Transactional
    public void delete(Long userId, Long draftId, Long imageId) {
        TravelPost draft = ownedDraft(userId, draftId);
        TravelPostImage image = imageRepository.findByIdAndPostId(imageId, draftId)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.IMAGE_NOT_FOUND));
        String objectKey = image.getObjectKey();
        imageRepository.delete(image);
        imageRepository.flush();
        normalizeOrders(draftId);
        draft.touchDraft();
        travelPostRepository.flush();
        deletionQueue.schedule(objectKey);
    }

    @Transactional
    public void deletePublished(Long userId, Long postId, Long imageId) {
        TravelPost post = ownedPublished(userId, postId);
        TravelPostImage image = imageRepository.findByIdAndPostId(imageId, postId)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.IMAGE_NOT_FOUND));
        String objectKey = image.getObjectKey();
        imageRepository.delete(image);
        imageRepository.flush();
        normalizeOrders(postId);
        post.touchPublished();
        travelPostRepository.flush();
        deletionQueue.schedule(objectKey);
    }

    @Transactional
    public List<DraftImageItem> reorder(Long userId, Long draftId,
                                        ReorderTravelPostImagesRequest request) {
        TravelPost draft = ownedDraft(userId, draftId);
        List<TravelPostImage> images = imageRepository.findAllByPostIdOrderByDisplayOrderAsc(draftId);
        if (request.imageIds().size() != images.size()
                || new HashSet<>(request.imageIds()).size() != images.size()
                || !new HashSet<>(request.imageIds()).equals(images.stream()
                .map(TravelPostImage::getId).collect(java.util.stream.Collectors.toSet()))) {
            throw new CommunityException(CommunityErrorCode.INVALID_IMAGE_ORDER);
        }
        var byId = images.stream().collect(java.util.stream.Collectors.toMap(
                TravelPostImage::getId, image -> image));
        for (int index = 0; index < request.imageIds().size(); index++) {
            byId.get(request.imageIds().get(index)).changeDisplayOrder((short) (index + 10));
        }
        imageRepository.flush();
        for (int index = 0; index < request.imageIds().size(); index++) {
            byId.get(request.imageIds().get(index)).changeDisplayOrder((short) index);
        }
        imageRepository.flush();
        draft.touchDraft();
        travelPostRepository.flush();
        return request.imageIds().stream().map(byId::get)
                .map(image -> DraftImageItem.from(image,
                        imageStorage.accessUrl(image.getObjectKey()))).toList();
    }

    @Transactional
    public List<TravelPostDetailResponse.PostImageItem> reorderPublished(
            Long userId, Long postId, ReorderTravelPostImagesRequest request) {
        TravelPost post = ownedPublished(userId, postId);
        List<TravelPostImage> images = reorderEntities(postId, request);
        post.touchPublished();
        travelPostRepository.flush();
        return images.stream().map(image -> new TravelPostDetailResponse.PostImageItem(
                image.getId(), imageStorage.accessUrl(image.getObjectKey()), image.getAltText(),
                image.getDisplayOrder())).toList();
    }

    private TravelPost ownedDraft(Long userId, Long draftId) {
        return travelPostRepository.findOwnedByIdAndStatusForUpdate(
                        draftId, userId, TravelPostStatus.DRAFT)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.DRAFT_NOT_FOUND));
    }

    private TravelPost ownedPublished(Long userId, Long postId) {
        TravelPost post = travelPostRepository.findByIdAndStatusForUpdate(
                        postId, TravelPostStatus.PUBLISHED)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.POST_NOT_FOUND));
        if (!post.getAuthor().getId().equals(userId)) {
            throw new CommunityException(CommunityErrorCode.POST_PERMISSION_REQUIRED);
        }
        return post;
    }

    private List<TravelPostImage> reorderEntities(Long postId,
                                                   ReorderTravelPostImagesRequest request) {
        List<TravelPostImage> images = imageRepository.findAllByPostIdOrderByDisplayOrderAsc(postId);
        var requested = new HashSet<>(request.imageIds());
        var actual = images.stream().map(TravelPostImage::getId)
                .collect(java.util.stream.Collectors.toSet());
        if (request.imageIds().size() != images.size()
                || requested.size() != images.size() || !requested.equals(actual)) {
            throw new CommunityException(CommunityErrorCode.INVALID_IMAGE_ORDER);
        }
        var byId = images.stream().collect(java.util.stream.Collectors.toMap(
                TravelPostImage::getId, image -> image));
        for (int index = 0; index < request.imageIds().size(); index++) {
            byId.get(request.imageIds().get(index)).changeDisplayOrder((short) (index + 10));
        }
        imageRepository.flush();
        for (int index = 0; index < request.imageIds().size(); index++) {
            byId.get(request.imageIds().get(index)).changeDisplayOrder((short) index);
        }
        imageRepository.flush();
        return request.imageIds().stream().map(byId::get).toList();
    }

    private void normalizeOrders(Long draftId) {
        List<TravelPostImage> images = imageRepository.findAllByPostIdOrderByDisplayOrderAsc(draftId);
        for (int index = 0; index < images.size(); index++) {
            images.get(index).changeDisplayOrder((short) (index + 10));
        }
        imageRepository.flush();
        for (int index = 0; index < images.size(); index++) {
            images.get(index).changeDisplayOrder((short) index);
        }
        imageRepository.flush();
    }

    private void deleteStoredFileOnRollback(String objectKey) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    deletionQueue.scheduleAfterRollback(objectKey);
                }
            }
        });
    }
}
