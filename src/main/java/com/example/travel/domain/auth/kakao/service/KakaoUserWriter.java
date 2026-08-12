package com.example.travel.domain.auth.kakao.service;

import com.example.travel.domain.auth.kakao.dto.KakaoUserResponse;
import com.example.travel.domain.auth.kakao.exception.KakaoErrorCode;
import com.example.travel.domain.auth.kakao.exception.KakaoException;
import com.example.travel.domain.user.enums.AuthProvider;
import com.example.travel.domain.user.entity.SocialAccount;
import com.example.travel.domain.user.repository.SocialAccountRepository;
import com.example.travel.domain.user.entity.User;
import com.example.travel.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KakaoUserWriter {
    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;

    public KakaoUserWriter(UserRepository userRepository,
                           SocialAccountRepository socialAccountRepository) {
        this.userRepository = userRepository;
        this.socialAccountRepository = socialAccountRepository;
    }

    @Transactional
    public Long findOrCreate(KakaoUserResponse kakaoUser) {
        String providerUserId = kakaoUser.id().toString();
        String email = kakaoUser.verifiedEmail().orElseThrow(() ->
                new KakaoException(KakaoErrorCode.EMAIL_REQUIRED));

        return socialAccountRepository
                .findByProviderAndProviderUserId(AuthProvider.KAKAO, providerUserId)
                .map(account -> updateAndGetUserId(account, email, kakaoUser))
                .orElseGet(() -> create(kakaoUser, providerUserId, email));
    }

    private Long create(KakaoUserResponse kakaoUser, String providerUserId, String email) {
        User user = userRepository.save(User.createSocial(
                kakaoUser.nicknameOrDefault(),
                kakaoUser.profileImageUrl().orElse(null)));
        user.recordLogin();
        socialAccountRepository.saveAndFlush(SocialAccount.create(
                user, AuthProvider.KAKAO, providerUserId, email, true));
        return user.getId();
    }

    private Long updateAndGetUserId(SocialAccount account, String email, KakaoUserResponse kakaoUser) {
        if (!email.equalsIgnoreCase(account.getProviderEmail()) || !account.isEmailVerified()) {
            account.updateEmail(email, true);
        }
        kakaoUser.profileImageUrl().ifPresent(account.getUser()::updateAvatarUrl);
        account.getUser().recordLogin();
        return account.getUser().getId();
    }
}
