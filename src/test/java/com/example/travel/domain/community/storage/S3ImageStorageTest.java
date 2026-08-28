package com.example.travel.domain.community.storage;

import com.example.travel.domain.community.config.CommunityImageProperties;
import com.example.travel.domain.community.exception.CommunityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class S3ImageStorageTest {
    private S3Client client;
    private S3Presigner presigner;
    private S3ImageStorage storage;

    @BeforeEach
    void setUp() {
        client = mock(S3Client.class);
        presigner = mock(S3Presigner.class);
        storage = new S3ImageStorage(client, presigner,
                new CommunityImageProperties("bucket", "ap-northeast-2", 30, 10, 5));
    }

    @Test
    void convertsClientFailureWhileStoring() {
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(SdkClientException.create("unavailable"));

        assertCode(() -> storage.store("key", new byte[]{1}, "image/png"),
                "COMMUNITY_500_IMAGE_STORAGE_FAILED");
    }

    @Test
    void convertsClientFailureWhileDeleting() {
        doThrow(SdkClientException.create("unavailable"))
                .when(client).deleteObject(any(DeleteObjectRequest.class));

        assertCode(() -> storage.delete("key"), "COMMUNITY_500_IMAGE_DELETE_FAILED");
    }

    @Test
    void convertsClientFailureWhileCreatingAccessUrl() {
        when(presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenThrow(SdkClientException.create("unavailable"));

        assertCode(() -> storage.accessUrl("key"), "COMMUNITY_500_IMAGE_STORAGE_FAILED");
    }

    private void assertCode(Runnable operation, String code) {
        assertThatThrownBy(operation::run)
                .isInstanceOfSatisfying(CommunityException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(code));
    }
}
