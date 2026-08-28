package com.example.travel.domain.community.service.cleanup;

import com.example.travel.domain.community.repository.S3ObjectDeletionTaskRepository;
import com.example.travel.domain.community.storage.ImageStorage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:s3-deletion-queue;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class S3ObjectDeletionQueueIntegrationTest {
    @Autowired
    private S3ObjectDeletionQueue queue;

    @Autowired
    private S3ObjectDeletionTaskRepository repository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockitoBean
    private ImageStorage imageStorage;

    @Test
    void rollbackCleanupDeletesAfterDeletionTaskCommits() {
        String objectKey = "community/posts/1/rolled-back.png";

        transactionTemplate.executeWithoutResult(status -> {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCompletion(int completionStatus) {
                            if (completionStatus == STATUS_ROLLED_BACK) {
                                queue.scheduleAfterRollback(objectKey);
                            }
                        }
                    });
            status.setRollbackOnly();
        });

        verify(imageStorage).delete(objectKey);
        assertThat(repository.findByObjectKey(objectKey)).isEmpty();
    }
}
