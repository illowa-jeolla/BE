package com.example.travel.domain.auth.google.service;

import com.example.travel.domain.auth.google.dto.GoogleUserInfo;
import com.example.travel.domain.auth.google.exception.GoogleErrorCode;
import com.example.travel.domain.auth.google.exception.GoogleException;
import com.example.travel.domain.user.entity.SocialAccount;
import com.example.travel.domain.user.entity.User;
import com.example.travel.domain.user.enums.AuthProvider;
import com.example.travel.domain.user.repository.SocialAccountRepository;
import com.example.travel.domain.user.repository.UserRepository;
import com.example.travel.domain.user.service.SocialNicknameGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoogleUserWriter {
    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final SocialNicknameGenerator nicknameGenerator;

    public GoogleUserWriter(UserRepository userRepository,
                            SocialAccountRepository socialAccountRepository,
                            SocialNicknameGenerator nicknameGenerator) {
        this.userRepository = userRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.nicknameGenerator = nicknameGenerator;
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
        String nickname = nicknameGenerator.generate(
                googleUser.nicknameOrDefault(), AuthProvider.GOOGLE, googleUser.subject());
        User user = userRepository.save(User.createSocial(
                nickname, googleUser.picture()));
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
