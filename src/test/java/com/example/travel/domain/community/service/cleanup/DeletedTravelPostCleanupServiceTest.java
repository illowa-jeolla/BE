package com.example.travel.domain.community.service.cleanup;

import com.example.travel.domain.community.entity.TravelPost;
import com.example.travel.domain.community.entity.TravelPostImage;
import com.example.travel.domain.community.enums.TravelPostStatus;
import com.example.travel.domain.community.repository.TravelPostCommentRepository;
import com.example.travel.domain.community.repository.TravelPostImageRepository;
import com.example.travel.domain.community.repository.TravelPostLikeRepository;
import com.example.travel.domain.community.repository.TravelPostRepository;
import com.example.travel.domain.community.repository.TravelPostTagRepository;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeletedTravelPostCleanupServiceTest {
    @Test
    void deletesExpiredPostsAndSchedulesTheirImages() {
        TravelPostRepository postRepository = mock(TravelPostRepository.class);
        TravelPostImageRepository imageRepository = mock(TravelPostImageRepository.class);
        TravelPostCommentRepository commentRepository = mock(TravelPostCommentRepository.class);
        TravelPostLikeRepository likeRepository = mock(TravelPostLikeRepository.class);
        TravelPostTagRepository tagRepository = mock(TravelPostTagRepository.class);
        S3ObjectDeletionQueue deletionQueue = mock(S3ObjectDeletionQueue.class);
        TravelPost post = mock(TravelPost.class);
        TravelPostImage firstImage = mock(TravelPostImage.class);
        TravelPostImage secondImage = mock(TravelPostImage.class);
        OffsetDateTime cutoff = OffsetDateTime.parse("2026-08-21T04:30:00+09:00");

        when(post.getId()).thenReturn(15L);
        when(firstImage.getObjectKey()).thenReturn("community/posts/15/one.png");
        when(secondImage.getObjectKey()).thenReturn("community/posts/15/two.png");
        when(postRepository.findAllByStatusAndUpdatedAtBefore(
                TravelPostStatus.DELETED, cutoff)).thenReturn(List.of(post));
        when(imageRepository.findAllByPostIdOrderByDisplayOrderAsc(15L))
                .thenReturn(List.of(firstImage, secondImage));

        DeletedTravelPostCleanupService service = new DeletedTravelPostCleanupService(
                postRepository, imageRepository, commentRepository, likeRepository,
                tagRepository, deletionQueue);

        assertThat(service.deleteExpired(cutoff)).isEqualTo(1);
        verify(commentRepository).deleteAllByPostId(15L);
        verify(likeRepository).deleteAllByPostId(15L);
        verify(tagRepository).deleteAllByPostId(15L);
        verify(imageRepository).deleteAllByPostId(15L);
        verify(postRepository).deleteAll(List.of(post));
        verify(deletionQueue).schedule("community/posts/15/one.png");
        verify(deletionQueue).schedule("community/posts/15/two.png");
        verify(postRepository).flush();
    }
}
