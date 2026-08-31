package com.example.travel.domain.community.storage;

import com.example.travel.domain.community.config.LocalImageProperties;
import com.example.travel.domain.community.exception.CommunityException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalImageStorageTest {
    @TempDir
    Path directory;

    @Test
    void storesServesAndDeletesAnImage() throws Exception {
        LocalImageStorage storage = new LocalImageStorage(new LocalImageProperties(
                directory.toString(), "http://localhost:8080/local-images/"));
        String objectKey = "community/posts/1/image.png";

        storage.store(objectKey, new byte[]{1, 2, 3}, "image/png");

        Path stored = directory.resolve(objectKey);
        assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(stored));
        assertEquals("http://localhost:8080/local-images/community/posts/1/image.png",
                storage.accessUrl(objectKey));

        storage.delete(objectKey);
        assertFalse(Files.exists(stored));
    }

    @Test
    void rejectsPathTraversal() {
        LocalImageStorage storage = new LocalImageStorage(new LocalImageProperties(
                directory.toString(), "http://localhost:8080/local-images"));

        assertThrows(CommunityException.class,
                () -> storage.store("../outside.png", new byte[]{1}, "image/png"));
    }
}
