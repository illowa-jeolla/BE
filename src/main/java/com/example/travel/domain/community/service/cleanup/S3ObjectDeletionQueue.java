package com.example.travel.domain.community.service.cleanup;

import com.example.travel.domain.community.entity.S3ObjectDeletionTask;
import com.example.travel.domain.community.repository.S3ObjectDeletionTaskRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class S3ObjectDeletionQueue {
    private final S3ObjectDeletionTaskRepository repository;
    private final S3ObjectDeletionProcessor processor;

    public S3ObjectDeletionQueue(S3ObjectDeletionTaskRepository repository,
                                 S3ObjectDeletionProcessor processor) {
        this.repository = repository;
        this.processor = processor;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void schedule(String objectKey) {
        if (repository.findByObjectKey(objectKey).isEmpty()) {
            repository.save(S3ObjectDeletionTask.create(objectKey));
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                processor.processOne(objectKey);
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void scheduleAfterRollback(String objectKey) {
        try {
            if (repository.findByObjectKey(objectKey).isEmpty()) {
                repository.saveAndFlush(S3ObjectDeletionTask.create(objectKey));
            }
        } catch (DataIntegrityViolationException ignored) {
            // 동일 objectKey 삭제 작업이 이미 등록됐다.
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                processor.processOne(objectKey);
            }
        });
    }
}
