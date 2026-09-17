package com.example.travel.domain.community.storage;

import com.example.travel.domain.community.config.CommunityImageProperties;
import com.example.travel.domain.community.exception.CommunityException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageFileValidatorTest {
    private final ImageFileValidator validator = new ImageFileValidator(
            new CommunityImageProperties("bucket", "ap-northeast-2", 30, 10, 5));

    @Test
    void acceptsPngWithMatchingSignature() {
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

        var result = validator.validate(new MockMultipartFile(
                "file", "image.png", "image/png", png));

        assertThat(result.extension()).isEqualTo("png");
        assertThat(result.content()).isEqualTo(png);
    }

    @Test
    void detectsPngFromGenericBinaryContentType() {
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

        var result = validator.validate(new MockMultipartFile(
                "file", "하이여.png", "application/octet-stream", png));

        assertThat(result.contentType()).isEqualTo("image/png");
        assertThat(result.extension()).isEqualTo("png");
    }

    @Test
    void acceptsContentTypeWithParameters() {
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

        var result = validator.validate(new MockMultipartFile(
                "file", "image.png", "image/png; charset=binary", png));

        assertThat(result.contentType()).isEqualTo("image/png");
    }

    @Test
    void rejectsContentTypeThatDoesNotMatchSignature() {
        assertThatThrownBy(() -> validator.validate(new MockMultipartFile(
                "file", "fake.png", "image/png", new byte[]{1, 2, 3})))
                .isInstanceOfSatisfying(CommunityException.class, exception ->
                        assertThat(exception.getCode())
                                .isEqualTo("COMMUNITY_400_INVALID_IMAGE_SIGNATURE"));
    }

    @Test
    void rejectsFileOverConfiguredLimit() {
        assertThatThrownBy(() -> validator.validate(new MockMultipartFile(
                "file", "large.jpg", "image/jpeg", new byte[11])))
                .isInstanceOfSatisfying(CommunityException.class, exception ->
                        assertThat(exception.getCode())
                                .isEqualTo("COMMUNITY_400_IMAGE_TOO_LARGE"));
    }
}
