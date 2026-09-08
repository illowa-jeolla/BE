package com.example.travel.domain.community.service;

import com.example.travel.domain.community.enums.TravelPostStatus;
import com.example.travel.domain.community.exception.CommunityException;
import com.example.travel.domain.community.repository.TravelPostCommentRepository;
import com.example.travel.domain.community.repository.TravelPostImageRepository;
import com.example.travel.domain.community.repository.TravelPostRepository;
import com.example.travel.domain.region.repository.RegionRepository;
import com.example.travel.domain.community.storage.ImageStorage;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TravelPostServiceTest {
    @Test
    void findsOnlyCurrentUsersPublishedPosts() {
        TravelPostRepository postRepository = mock(TravelPostRepository.class);
        when(postRepository.findPublishedByAuthor(
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq(TravelPostStatus.PUBLISHED),
                org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        TravelPostService service = new TravelPostService(postRepository,
                mock(TravelPostImageRepository.class),
                mock(TravelPostCommentRepository.class), mock(RegionRepository.class),
                mock(ImageStorage.class), mock(TravelPostImageOrderService.class));

        var response = service.findMine(7L, 0, 20);

        assertThat(response.content()).isEmpty();
        verify(postRepository).findPublishedByAuthor(
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq(TravelPostStatus.PUBLISHED),
                org.mockito.ArgumentMatchers.argThat(pageable ->
                        pageable.getPageNumber() == 0 && pageable.getPageSize() == 20));
    }

    @Test
    void rejectsDetailWhenPublishedPostDoesNotExist() {
        TravelPostRepository postRepository = mock(TravelPostRepository.class);
        when(postRepository.incrementViewCount(15L, TravelPostStatus.PUBLISHED)).thenReturn(0);
        TravelPostService service = new TravelPostService(postRepository,
                mock(TravelPostImageRepository.class),
                mock(TravelPostCommentRepository.class), mock(RegionRepository.class),
                mock(ImageStorage.class), mock(TravelPostImageOrderService.class));

        assertThatThrownBy(() -> service.findDetail(15L, 7L))
                .isInstanceOfSatisfying(CommunityException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("COMMUNITY_404_POST_NOT_FOUND"));
    }
}
