package com.example.travel.domain.community.service.cleanup;

import com.example.travel.domain.community.entity.S3ObjectDeletionTask;
import com.example.travel.domain.community.repository.S3ObjectDeletionTaskRepository;
import com.example.travel.domain.community.storage.ImageStorage;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class S3ObjectDeletionProcessorTest {
    private final S3ObjectDeletionTaskRepository repository =
            mock(S3ObjectDeletionTaskRepository.class);
    private final ImageStorage storage = mock(ImageStorage.class);
    private final S3ObjectDeletionProcessor processor =
            new S3ObjectDeletionProcessor(repository, storage);

    @Test
    void removesTaskAfterS3DeletionSucceeds() {
        var task = S3ObjectDeletionTask.create("community/posts/1/image.png");
        when(repository.findByNextAttemptAtLessThanEqualOrderByIdAsc(
                any(OffsetDateTime.class), any(Pageable.class))).thenReturn(List.of(task));

        assertThat(processor.processDue(OffsetDateTime.now())).isEqualTo(1);

        verify(storage).delete(task.getObjectKey());
        verify(repository).delete(task);
    }

    @Test
    void keepsAndReschedulesTaskWhenS3DeletionFails() {
        var task = S3ObjectDeletionTask.create("community/posts/1/image.png");
        when(repository.findByNextAttemptAtLessThanEqualOrderByIdAsc(
                any(OffsetDateTime.class), any(Pageable.class))).thenReturn(List.of(task));
        doThrow(new RuntimeException("S3 unavailable")).when(storage).delete(task.getObjectKey());

        processor.processDue(OffsetDateTime.now());

        assertThat(task.getAttemptCount()).isEqualTo(1);
        verify(repository, never()).delete(task);
    }
}
