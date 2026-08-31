package com.example.travel.domain.community.storage;

import com.example.travel.domain.community.config.CommunityImageProperties;
import com.example.travel.domain.community.exception.CommunityErrorCode;
import com.example.travel.domain.community.exception.CommunityException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

@Component
public class ImageFileValidator {
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    private final long maxFileSize;

    public ImageFileValidator(CommunityImageProperties properties) {
        this.maxFileSize = properties.maxFileSize();
    }

    public ValidatedImage validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CommunityException(CommunityErrorCode.EMPTY_IMAGE);
        }
        if (file.getSize() > maxFileSize) {
            throw new CommunityException(CommunityErrorCode.IMAGE_TOO_LARGE);
        }
        try {
            byte[] content = file.getBytes();
            String contentType = normalizedContentType(file.getContentType());
            if (contentType == null || "application/octet-stream".equals(contentType)) {
                contentType = detectContentType(content);
            }
            String extension = EXTENSIONS.get(contentType);
            if (extension == null) {
                throw new CommunityException(CommunityErrorCode.UNSUPPORTED_IMAGE_TYPE);
            }
            if (!matchesSignature(contentType, content)) {
                throw new CommunityException(CommunityErrorCode.INVALID_IMAGE_SIGNATURE);
            }
            return new ValidatedImage(content, extension, contentType);
        } catch (IOException exception) {
            throw new CommunityException(CommunityErrorCode.IMAGE_READ_FAILED);
        }
    }

    private String normalizedContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return null;
        }
        int parameterStart = contentType.indexOf(';');
        String mediaType = parameterStart >= 0
                ? contentType.substring(0, parameterStart)
                : contentType;
        return mediaType.trim().toLowerCase(Locale.ROOT);
    }

    private String detectContentType(byte[] bytes) {
        for (String supportedContentType : EXTENSIONS.keySet()) {
            if (matchesSignature(supportedContentType, bytes)) {
                return supportedContentType;
            }
        }
        return null;
    }

    private boolean matchesSignature(String contentType, byte[] bytes) {
        return switch (contentType) {
            case "image/jpeg" -> bytes.length >= 3
                    && unsigned(bytes[0]) == 0xFF
                    && unsigned(bytes[1]) == 0xD8
                    && unsigned(bytes[2]) == 0xFF;
            case "image/png" -> bytes.length >= 8
                    && unsigned(bytes[0]) == 0x89 && bytes[1] == 0x50
                    && bytes[2] == 0x4E && bytes[3] == 0x47
                    && bytes[4] == 0x0D && bytes[5] == 0x0A
                    && bytes[6] == 0x1A && bytes[7] == 0x0A;
            case "image/webp" -> bytes.length >= 12
                    && bytes[0] == 'R' && bytes[1] == 'I'
                    && bytes[2] == 'F' && bytes[3] == 'F'
                    && bytes[8] == 'W' && bytes[9] == 'E'
                    && bytes[10] == 'B' && bytes[11] == 'P';
            default -> false;
        };
    }

    private int unsigned(byte value) {
        return value & 0xFF;
    }

    public record ValidatedImage(byte[] content, String extension, String contentType) {
    }
}
