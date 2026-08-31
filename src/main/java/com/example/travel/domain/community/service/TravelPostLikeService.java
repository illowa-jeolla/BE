package com.example.travel.domain.community.service;

import com.example.travel.domain.community.dto.response.TravelPostLikeResponse;
import com.example.travel.domain.community.entity.TravelPost;
import com.example.travel.domain.community.entity.TravelPostLike;
import com.example.travel.domain.community.entity.id.TravelPostLikeId;
import com.example.travel.domain.community.enums.TravelPostStatus;
import com.example.travel.domain.community.exception.CommunityErrorCode;
import com.example.travel.domain.community.exception.CommunityException;
import com.example.travel.domain.community.repository.TravelPostLikeRepository;
import com.example.travel.domain.community.repository.TravelPostRepository;
import com.example.travel.domain.user.entity.User;
import com.example.travel.domain.user.enums.UserStatus;
import com.example.travel.domain.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TravelPostLikeService {
    private final TravelPostRepository postRepository;
    private final TravelPostLikeRepository likeRepository;
    private final UserRepository userRepository;

    public TravelPostLikeService(TravelPostRepository postRepository,
                                 TravelPostLikeRepository likeRepository,
                                 UserRepository userRepository) {
        this.postRepository = postRepository;
        this.likeRepository = likeRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TravelPostLikeResponse like(Long postId, Long userId) {
        TravelPost post = publishedForUpdate(postId);
        if (post.getAuthor().getId().equals(userId)) {
            throw new CommunityException(CommunityErrorCode.SELF_LIKE_FORBIDDEN);
        }
        User user = userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.USER_NOT_FOUND));
        TravelPostLikeId likeId = new TravelPostLikeId(postId, userId);
        if (likeRepository.existsById(likeId)) {
            throw new CommunityException(CommunityErrorCode.POST_ALREADY_LIKED);
        }
        try {
            likeRepository.saveAndFlush(TravelPostLike.create(post, user));
        } catch (DataIntegrityViolationException exception) {
            throw new CommunityException(CommunityErrorCode.POST_ALREADY_LIKED);
        }
        return new TravelPostLikeResponse(likeRepository.countByPostId(postId), true);
    }

    @Transactional
    public TravelPostLikeResponse unlike(Long postId, Long userId) {
        publishedForUpdate(postId);
        TravelPostLikeId likeId = new TravelPostLikeId(postId, userId);
        if (!likeRepository.existsById(likeId)) {
            throw new CommunityException(CommunityErrorCode.POST_LIKE_NOT_FOUND);
        }
        likeRepository.deleteById(likeId);
        likeRepository.flush();
        return new TravelPostLikeResponse(likeRepository.countByPostId(postId), false);
    }

    private TravelPost publishedForUpdate(Long postId) {
        return postRepository.findByIdAndStatusForUpdate(postId, TravelPostStatus.PUBLISHED)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.POST_NOT_FOUND));
    }
}
