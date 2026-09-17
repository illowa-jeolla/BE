package com.example.travel.domain.community.service;

import com.example.travel.domain.community.dto.request.CreateTravelPostCommentRequest;
import com.example.travel.domain.community.dto.request.UpdateTravelPostCommentRequest;
import com.example.travel.domain.community.dto.response.TravelPostCommentResponse;
import com.example.travel.domain.community.entity.TravelPost;
import com.example.travel.domain.community.entity.TravelPostComment;
import com.example.travel.domain.community.enums.CommentStatus;
import com.example.travel.domain.community.enums.TravelPostStatus;
import com.example.travel.domain.community.exception.CommunityErrorCode;
import com.example.travel.domain.community.exception.CommunityException;
import com.example.travel.domain.community.repository.TravelPostCommentRepository;
import com.example.travel.domain.community.repository.TravelPostRepository;
import com.example.travel.domain.user.entity.User;
import com.example.travel.domain.user.enums.UserStatus;
import com.example.travel.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TravelPostCommentService {
    private static final List<CommentStatus> ACTIVE =
            List.of(CommentStatus.VISIBLE, CommentStatus.HIDDEN);

    private final TravelPostRepository postRepository;
    private final TravelPostCommentRepository commentRepository;
    private final UserRepository userRepository;

    public TravelPostCommentService(TravelPostRepository postRepository,
                                    TravelPostCommentRepository commentRepository,
                                    UserRepository userRepository) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<TravelPostCommentResponse> findAll(Long postId, Long viewerId) {
        requirePublished(postId);
        return commentRepository.findAllByPostIdAndStatusInOrderByCreatedAtAscIdAsc(
                        postId, ACTIVE).stream()
                .map(comment -> TravelPostCommentResponse.from(comment, viewerId)).toList();
    }

    @Transactional
    public TravelPostCommentResponse create(Long postId, Long userId,
                                            CreateTravelPostCommentRequest request) {
        TravelPost post = requirePublished(postId);
        User author = userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.USER_NOT_FOUND));
        TravelPostComment comment = commentRepository.save(TravelPostComment.create(
                post, author, request.content().trim(), request.secret()));
        return TravelPostCommentResponse.from(comment, userId);
    }

    @Transactional
    public TravelPostCommentResponse update(Long postId, Long commentId, Long userId,
                                            UpdateTravelPostCommentRequest request) {
        TravelPostComment comment = activeForUpdate(postId, commentId);
        requireOwner(comment, userId);
        comment.update(request.content().trim(), request.secret());
        commentRepository.flush();
        return TravelPostCommentResponse.from(comment, userId);
    }

    @Transactional
    public void delete(Long postId, Long commentId, Long userId) {
        TravelPostComment comment = activeForUpdate(postId, commentId);
        requireOwner(comment, userId);
        comment.softDelete();
        commentRepository.flush();
    }

    private TravelPost requirePublished(Long postId) {
        return postRepository.findDetail(postId, TravelPostStatus.PUBLISHED)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.POST_NOT_FOUND));
    }

    private TravelPostComment activeForUpdate(Long postId, Long commentId) {
        return commentRepository.findActiveForUpdate(commentId, postId, CommentStatus.DELETED)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.COMMENT_NOT_FOUND));
    }

    private void requireOwner(TravelPostComment comment, Long userId) {
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new CommunityException(CommunityErrorCode.COMMENT_PERMISSION_REQUIRED);
        }
    }
}
