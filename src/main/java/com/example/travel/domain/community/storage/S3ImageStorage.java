package com.example.travel.domain.community.storage;

import com.example.travel.domain.community.config.CommunityImageProperties;
import com.example.travel.domain.community.exception.CommunityErrorCode;
import com.example.travel.domain.community.exception.CommunityException;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.time.Duration;

@Component
@ConditionalOnProperty(name = "community.image.storage", havingValue = "s3", matchIfMissing = true)
public class S3ImageStorage implements ImageStorage {
    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final String bucket;
    private final Duration urlExpiration;

    public S3ImageStorage(S3Client s3Client, S3Presigner presigner,
                          CommunityImageProperties properties) {
        this.s3Client = s3Client;
        this.presigner = presigner;
        this.bucket = properties.bucket();
        this.urlExpiration = Duration.ofMinutes(properties.presignedUrlExpirationMinutes());
    }

    @Override
    public void store(String objectKey, byte[] content, String contentType) {
        try {
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(bucket).key(objectKey).contentType(contentType).build(),
                    RequestBody.fromBytes(content));
        } catch (SdkException exception) {
            throw new CommunityException(CommunityErrorCode.IMAGE_STORAGE_FAILED);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket).key(objectKey).build());
        } catch (SdkException exception) {
            throw new CommunityException(CommunityErrorCode.IMAGE_DELETE_FAILED);
        }
    }

    @Override
    public String accessUrl(String objectKey) {
        try {
            var getObject = GetObjectRequest.builder().bucket(bucket).key(objectKey).build();
            return presigner.presignGetObject(GetObjectPresignRequest.builder()
                            .signatureDuration(urlExpiration).getObjectRequest(getObject).build())
                    .url().toString();
        } catch (SdkException exception) {
            throw new CommunityException(CommunityErrorCode.IMAGE_STORAGE_FAILED);
        }
    }
}
