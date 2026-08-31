package com.example.travel.domain.community.service;

import com.example.travel.domain.community.dto.request.TravelPostSearchRequest;
import com.example.travel.domain.community.dto.request.UpdateTravelPostRequest;
import com.example.travel.domain.community.dto.response.TravelPostDetailResponse;
import com.example.travel.domain.community.dto.response.TravelPostListItem;
import com.example.travel.domain.community.dto.response.TravelPostListResponse;
import com.example.travel.domain.community.entity.TravelPost;
import com.example.travel.domain.community.enums.CommentStatus;
import com.example.travel.domain.community.enums.TravelPostStatus;
import com.example.travel.domain.community.exception.CommunityErrorCode;
import com.example.travel.domain.community.exception.CommunityException;
import com.example.travel.domain.community.repository.TravelPostCommentRepository;
import com.example.travel.domain.community.repository.TravelPostImageRepository;
import com.example.travel.domain.community.repository.TravelPostRepository;
import com.example.travel.domain.region.entity.Region;
import com.example.travel.domain.region.repository.RegionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TravelPostService {
    private static final List<CommentStatus> COUNTED_COMMENTS =
            List.of(CommentStatus.VISIBLE, CommentStatus.HIDDEN);

    private final TravelPostRepository postRepository;
    private final TravelPostImageRepository imageRepository;
    private final TravelPostCommentRepository commentRepository;
    private final RegionRepository regionRepository;
    private final com.example.travel.domain.community.storage.ImageStorage imageStorage;
    private final TravelPostImageOrderService imageOrderService;

    public TravelPostService(TravelPostRepository postRepository,
                             TravelPostImageRepository imageRepository,
                             TravelPostCommentRepository commentRepository,
                             RegionRepository regionRepository,
                             com.example.travel.domain.community.storage.ImageStorage imageStorage,
                             TravelPostImageOrderService imageOrderService) {
        this.postRepository = postRepository;
        this.imageRepository = imageRepository;
        this.commentRepository = commentRepository;
        this.regionRepository = regionRepository;
        this.imageStorage = imageStorage;
        this.imageOrderService = imageOrderService;
    }

    @Transactional(readOnly = true)
    public TravelPostListResponse findAll(TravelPostSearchRequest request) {
        var pageable = PageRequest.of(request.pageOrDefault(), request.sizeOrDefault(),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        var page = postRepository.findPublished(TravelPostStatus.PUBLISHED,
                request.regionId(), pageable);
        var content = page.getContent().stream().map(post -> {
            var images = imageRepository.findAllByPostIdOrderByDisplayOrderAsc(post.getId());
            String thumbnailUrl = images.isEmpty() ? null
                    : imageStorage.accessUrl(images.get(0).getObjectKey());
            long comments = commentRepository.countByPostIdAndStatusIn(
                    post.getId(), COUNTED_COMMENTS);
            return TravelPostListItem.from(post, comments, thumbnailUrl);
        }).toList();
        return new TravelPostListResponse(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.hasNext());
    }

    @Transactional
    public TravelPostDetailResponse findDetail(Long postId, Long viewerId) {
        if (postRepository.incrementViewCount(postId, TravelPostStatus.PUBLISHED) == 0) {
            throw new CommunityException(CommunityErrorCode.POST_NOT_FOUND);
        }
        TravelPost post = postRepository.findDetail(postId, TravelPostStatus.PUBLISHED)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.POST_NOT_FOUND));
        return detail(post, viewerId);
    }

    @Transactional
    public TravelPostDetailResponse update(Long postId, Long userId,
                                           UpdateTravelPostRequest request) {
        TravelPost post = ownedPublished(postId, userId);
        Region region = regionRepository.findActiveById(request.regionId())
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.REGION_NOT_FOUND));
        post.updatePublished(region, request.title().trim(), normalize(request.concept()),
                request.content().trim());
        imageOrderService.reorder(postId, request.imageIds());
        postRepository.flush();
        return detail(post, userId);
    }

    @Transactional
    public void delete(Long postId, Long userId) {
        TravelPost post = ownedPublished(postId, userId);
        post.softDelete();
        postRepository.flush();
    }

    private TravelPost ownedPublished(Long postId, Long userId) {
        TravelPost post = postRepository.findByIdAndStatusForUpdate(
                        postId, TravelPostStatus.PUBLISHED)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.POST_NOT_FOUND));
        if (!post.getAuthor().getId().equals(userId)) {
            throw new CommunityException(CommunityErrorCode.POST_PERMISSION_REQUIRED);
        }
        return post;
    }

    private TravelPostDetailResponse detail(TravelPost post, Long viewerId) {
        var images = imageRepository.findAllByPostIdOrderByDisplayOrderAsc(post.getId()).stream()
                .map(image -> new TravelPostDetailResponse.PostImageItem(image.getId(),
                        imageStorage.accessUrl(image.getObjectKey()),
                        image.getAltText(), image.getDisplayOrder())).toList();
        long comments = commentRepository.countByPostIdAndStatusIn(post.getId(), COUNTED_COMMENTS);
        return TravelPostDetailResponse.from(post, viewerId, comments, images);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
