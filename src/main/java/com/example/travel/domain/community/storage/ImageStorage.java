package com.example.travel.domain.community.storage;

public interface ImageStorage {
    void store(String objectKey, byte[] content, String contentType);

    void delete(String objectKey);

    String accessUrl(String objectKey);
}
