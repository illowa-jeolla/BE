package com.example.travel.domain.community.service.cleanup;

import com.example.travel.domain.community.repository.S3ObjectDeletionTaskRepository;
import com.example.travel.domain.community.storage.ImageStorage;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class S3ObjectDeletionProcessor {
    private static final int BATCH_SIZE = 100;
    private final S3ObjectDeletionTaskRepository repository;
    private final ImageStorage imageStorage;

    public S3ObjectDeletionProcessor(S3ObjectDeletionTaskRepository repository,
                                     ImageStorage imageStorage) {
        this.repository = repository;
        this.imageStorage = imageStorage;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processOne(String objectKey) {
        repository.findByObjectKey(objectKey).ifPresent(this::deleteOrReschedule);
    }

    @Transactional
    public int processDue(OffsetDateTime now) {
        var tasks = repository.findByNextAttemptAtLessThanEqualOrderByIdAsc(
                now, PageRequest.of(0, BATCH_SIZE));
        tasks.forEach(this::deleteOrReschedule);
        return tasks.size();
    }

    private void deleteOrReschedule(
            com.example.travel.domain.community.entity.S3ObjectDeletionTask task) {
        try {
            imageStorage.delete(task.getObjectKey());
            repository.delete(task);
        } catch (RuntimeException exception) {
            long delayMinutes = Math.min(360, 1L << Math.min(task.getAttemptCount(), 8));
            task.recordFailure(exception.getMessage(), OffsetDateTime.now().plusMinutes(delayMinutes));
        }
    }
}
