package com.example.travel.domain.user.service;

import com.example.travel.domain.user.enums.AuthProvider;
import com.example.travel.domain.user.policy.NicknamePolicy;
import com.example.travel.domain.user.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class SocialNicknameGenerator {
    private static final int MAX_ATTEMPTS = 100;
    private static final int HASH_LENGTH = 6;

    private final UserRepository userRepository;

    public SocialNicknameGenerator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String generate(String requestedNickname, AuthProvider provider, String providerUserId) {
        String nickname = NicknamePolicy.truncate(requestedNickname.trim());
        if (!userRepository.existsByNickname(nickname)) {
            return nickname;
        }

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String suffix = "_" + hash(provider.name() + ":" + providerUserId + ":" + attempt);
            int baseLength = NicknamePolicy.MAX_LENGTH - suffix.length();
            String base = nickname.substring(0, Math.min(nickname.length(), baseLength));
            String candidate = base + suffix;
            if (!userRepository.existsByNickname(candidate)) {
                return candidate;
            }
        }

        throw new IllegalStateException("Unable to generate a unique social nickname");
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, HASH_LENGTH / 2);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
