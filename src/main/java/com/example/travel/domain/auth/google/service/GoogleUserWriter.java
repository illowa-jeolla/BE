package com.example.travel.domain.auth.google.service;

import com.example.travel.domain.auth.google.dto.GoogleUserInfo;
import com.example.travel.domain.auth.google.exception.GoogleErrorCode;
import com.example.travel.domain.auth.google.exception.GoogleException;
import com.example.travel.domain.user.entity.SocialAccount;
import com.example.travel.domain.user.entity.User;
import com.example.travel.domain.user.enums.AuthProvider;
import com.example.travel.domain.user.repository.SocialAccountRepository;
import com.example.travel.domain.user.repository.UserRepository;
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
            throw new GoogleException(GoogleErrorCode.INVALID_ID_TOKEN);
        }
        String email = googleUser.verifiedEmail();
        if (email == null) {
            throw new GoogleException(GoogleErrorCode.EMAIL_REQUIRED);
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
