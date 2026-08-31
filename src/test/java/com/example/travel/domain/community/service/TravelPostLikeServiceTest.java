package com.example.travel.domain.community.service;

import com.example.travel.domain.community.entity.TravelPost;
import com.example.travel.domain.community.entity.TravelPostLike;
import com.example.travel.domain.community.entity.id.TravelPostLikeId;
import com.example.travel.domain.community.enums.TravelPostStatus;
import com.example.travel.domain.community.exception.CommunityException;
import com.example.travel.domain.community.repository.TravelPostLikeRepository;
import com.example.travel.domain.community.repository.TravelPostRepository;
import com.example.travel.domain.user.entity.User;
import com.example.travel.domain.user.enums.UserStatus;
import com.example.travel.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TravelPostLikeServiceTest {
    private TravelPostRepository postRepository;
    private TravelPostLikeRepository likeRepository;
    private UserRepository userRepository;
    private TravelPostLikeService service;

    @BeforeEach
    void setUp() {
        postRepository = mock(TravelPostRepository.class);
        likeRepository = mock(TravelPostLikeRepository.class);
        userRepository = mock(UserRepository.class);
        service = new TravelPostLikeService(postRepository, likeRepository, userRepository);
    }

    @Test
    void likesPublishedPostOnce() {
        TravelPost post = publishedPost(15L, 3L);
        User user = user(7L);
        when(userRepository.findByIdAndStatus(7L, UserStatus.ACTIVE))
                .thenReturn(Optional.of(user));
        when(likeRepository.countByPostId(15L)).thenReturn(1L);

        var response = service.like(15L, 7L);

        assertThat(response.likeCount()).isEqualTo(1);
        assertThat(response.liked()).isTrue();
        verify(likeRepository).saveAndFlush(any(TravelPostLike.class));
    }

    @Test
    void rejectsDuplicateLike() {
        publishedPost(15L, 3L);
        User user = user(7L);
        when(userRepository.findByIdAndStatus(7L, UserStatus.ACTIVE))
                .thenReturn(Optional.of(user));
        when(likeRepository.existsById(new TravelPostLikeId(15L, 7L))).thenReturn(true);

        assertCode(() -> service.like(15L, 7L), "COMMUNITY_409_POST_ALREADY_LIKED");
        verify(likeRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsLikeFromPostAuthor() {
        publishedPost(15L, 7L);

        assertCode(() -> service.like(15L, 7L), "COMMUNITY_403_SELF_LIKE_FORBIDDEN");
        verify(userRepository, never()).findByIdAndStatus(any(), any());
    }

    @Test
    void rejectsLikeWhenPostIsNotPublished() {
        when(postRepository.findByIdAndStatusForUpdate(15L, TravelPostStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        assertCode(() -> service.like(15L, 7L), "COMMUNITY_404_POST_NOT_FOUND");
    }

    @Test
    void removesExistingLike() {
        publishedPost(15L, 3L);
        TravelPostLikeId likeId = new TravelPostLikeId(15L, 7L);
        when(likeRepository.existsById(likeId)).thenReturn(true);
        when(likeRepository.countByPostId(15L)).thenReturn(0L);

        var response = service.unlike(15L, 7L);

        verify(likeRepository).deleteById(likeId);
        assertThat(response.likeCount()).isZero();
        assertThat(response.liked()).isFalse();
    }

    private TravelPost publishedPost(Long postId, Long authorId) {
        TravelPost post = mock(TravelPost.class);
        User author = user(authorId);
        when(post.getId()).thenReturn(postId);
        when(post.getAuthor()).thenReturn(author);
        when(postRepository.findByIdAndStatusForUpdate(postId, TravelPostStatus.PUBLISHED))
                .thenReturn(Optional.of(post));
        return post;
    }

    private User user(Long userId) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        return user;
    }

    private void assertCode(Runnable operation, String expectedCode) {
        assertThatThrownBy(operation::run)
                .isInstanceOfSatisfying(CommunityException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(expectedCode));
    }
}
