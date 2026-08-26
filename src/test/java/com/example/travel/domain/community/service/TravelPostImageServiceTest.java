package com.example.travel.domain.community.service;

import com.example.travel.domain.community.config.CommunityImageProperties;
import com.example.travel.domain.community.entity.TravelPost;
import com.example.travel.domain.community.entity.TravelPostImage;
import com.example.travel.domain.community.enums.TravelPostStatus;
import com.example.travel.domain.community.exception.CommunityException;
import com.example.travel.domain.community.repository.TravelPostImageRepository;
import com.example.travel.domain.community.repository.TravelPostRepository;
import com.example.travel.domain.community.storage.ImageFileValidator;
import com.example.travel.domain.community.storage.ImageStorage;
import com.example.travel.domain.community.service.cleanup.S3ObjectDeletionQueue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TravelPostImageServiceTest {
    private TravelPostRepository postRepository;
    private TravelPostImageRepository imageRepository;
    private ImageFileValidator validator;
    private ImageStorage storage;
    private TravelPostImageService service;
    private S3ObjectDeletionQueue deletionQueue;

    @BeforeEach
    void setUp() {
        postRepository = mock(TravelPostRepository.class);
        imageRepository = mock(TravelPostImageRepository.class);
        validator = mock(ImageFileValidator.class);
        storage = mock(ImageStorage.class);
        deletionQueue = mock(S3ObjectDeletionQueue.class);
        service = new TravelPostImageService(postRepository, imageRepository, validator,
                storage, new CommunityImageProperties("bucket", "ap-northeast-2", 30, 10, 5),
                deletionQueue);
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void uploadsValidatedImageAndConnectsItToDraft() {
        TravelPost draft = mock(TravelPost.class);
        MultipartFile file = mock(MultipartFile.class);
        byte[] content = {1, 2, 3};
        when(postRepository.findOwnedByIdAndStatusForUpdate(
                15L, 7L, TravelPostStatus.DRAFT)).thenReturn(Optional.of(draft));
        when(imageRepository.countByPostId(15L)).thenReturn(2L);
        when(validator.validate(file)).thenReturn(
                new ImageFileValidator.ValidatedImage(content, "png", "image/png"));
        when(storage.accessUrl(anyString())).thenReturn("https://example.test/image");
        when(imageRepository.saveAndFlush(any(TravelPostImage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.upload(7L, 15L, file);

        assertThat(response.objectKey()).startsWith("community/posts/15/").endsWith(".png");
        assertThat(response.displayOrder()).isEqualTo((short) 2);
        verify(storage).store(response.objectKey(), content, "image/png");
        verify(draft).touchDraft();
        verify(postRepository).flush();
    }

    @Test
    void rejectsSixthImageBeforeReadingOrStoringFile() {
        TravelPost draft = mock(TravelPost.class);
        when(postRepository.findOwnedByIdAndStatusForUpdate(
                15L, 7L, TravelPostStatus.DRAFT)).thenReturn(Optional.of(draft));
        when(imageRepository.countByPostId(15L)).thenReturn(5L);

        assertThatThrownBy(() -> service.upload(7L, 15L, mock(MultipartFile.class)))
                .isInstanceOfSatisfying(CommunityException.class, exception ->
                        assertThat(exception.getCode())
                                .isEqualTo("COMMUNITY_400_IMAGE_LIMIT_EXCEEDED"));
        verify(validator, never()).validate(any());
        verify(storage, never()).store(anyString(), any(), anyString());
    }
}
