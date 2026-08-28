package com.example.travel.domain.community.storage;

import com.example.travel.domain.community.config.LocalImageProperties;
import com.example.travel.domain.community.exception.CommunityErrorCode;
import com.example.travel.domain.community.exception.CommunityException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Component
@ConditionalOnProperty(name = "community.image.storage", havingValue = "local")
public class LocalImageStorage implements ImageStorage {
    private final Path rootDirectory;
    private final String baseUrl;

    public LocalImageStorage(LocalImageProperties properties) {
        this.rootDirectory = Path.of(properties.directory()).toAbsolutePath().normalize();
        this.baseUrl = stripTrailingSlash(properties.baseUrl());
    }

    @Override
    public void store(String objectKey, byte[] content, String contentType) {
        Path target = resolveSafely(objectKey);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content, StandardOpenOption.CREATE_NEW);
        } catch (IOException exception) {
            throw new CommunityException(CommunityErrorCode.IMAGE_STORAGE_FAILED);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            Files.deleteIfExists(resolveSafely(objectKey));
        } catch (IOException exception) {
            throw new CommunityException(CommunityErrorCode.IMAGE_DELETE_FAILED);
        }
    }

    @Override
    public String accessUrl(String objectKey) {
        String normalizedKey = rootDirectory.relativize(resolveSafely(objectKey))
                .toString().replace('\\', '/');
        return baseUrl + "/" + normalizedKey;
    }

    Path rootDirectory() {
        return rootDirectory;
    }

    private Path resolveSafely(String objectKey) {
        Path resolved = rootDirectory.resolve(objectKey).normalize();
        if (!resolved.startsWith(rootDirectory)) {
            throw new CommunityException(CommunityErrorCode.IMAGE_STORAGE_FAILED);
        }
        return resolved;
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
