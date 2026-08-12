package com.example.travel.domain.auth.google.service;

import com.example.travel.domain.auth.google.dto.GoogleUserInfo;
import com.example.travel.domain.user.entity.SocialAccount;
import com.example.travel.domain.user.entity.User;
import com.example.travel.domain.user.enums.AuthProvider;
import com.example.travel.domain.user.repository.SocialAccountRepository;
import com.example.travel.domain.user.repository.UserRepository;
import com.example.travel.global.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoogleUserWriter {
    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;

    public GoogleUserWriter(UserRepository userRepository,
                            SocialAccountRepository socialAccountRepository) {
        this.userRepository = userRepository;
        this.socialAccountRepository = socialAccountRepository;
    }

    @Transactional
    public Long findOrCreate(GoogleUserInfo googleUser) {
        if (googleUser.subject() == null || googleUser.subject().isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "GOOGLE_401_INVALID_ID_TOKEN",
                    "유효하지 않은 Google ID Token입니다.");
        }
        String email = googleUser.verifiedEmail();
        if (email == null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "GOOGLE_422_EMAIL_REQUIRED",
                    "Google 계정의 인증된 이메일이 필요합니다.");
        }

        return socialAccountRepository
                .findByProviderAndProviderUserId(AuthProvider.GOOGLE, googleUser.subject())
                .map(account -> updateAndGetUserId(account, email))
                .orElseGet(() -> create(googleUser, email));
    }

    private Long create(GoogleUserInfo googleUser, String email) {
        User user = userRepository.save(User.createSocial(
                googleUser.nicknameOrDefault(), googleUser.picture()));
        user.recordLogin();
        socialAccountRepository.saveAndFlush(SocialAccount.create(
                user, AuthProvider.GOOGLE, googleUser.subject(), email, true));
        return user.getId();
    }

    private Long updateAndGetUserId(SocialAccount account, String email) {
        if (!email.equalsIgnoreCase(account.getProviderEmail()) || !account.isEmailVerified()) {
            account.updateEmail(email, true);
        }
        account.getUser().recordLogin();
        return account.getUser().getId();
    }
}
