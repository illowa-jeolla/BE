package com.example.travel.domain.auth.kakao;

import com.example.travel.domain.auth.kakao.dto.KakaoUserResponse;
import com.example.travel.domain.user.AuthProvider;
import com.example.travel.domain.user.SocialAccount;
import com.example.travel.domain.user.SocialAccountRepository;
import com.example.travel.domain.user.User;
import com.example.travel.domain.user.UserRepository;
import com.example.travel.global.common.ApiException;
import org.springframework.http.HttpStatus;
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
                new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "KAKAO_422_EMAIL_REQUIRED",
                        "카카오 계정의 인증된 이메일 제공 동의가 필요합니다."));

        return socialAccountRepository
                .findByProviderAndProviderUserId(AuthProvider.KAKAO, providerUserId)
                .map(account -> updateAndGetUserId(account, email))
                .orElseGet(() -> create(kakaoUser, providerUserId, email));
    }

    private Long create(KakaoUserResponse kakaoUser, String providerUserId, String email) {
        User user = userRepository.save(User.createSocial(
                kakaoUser.nicknameOrDefault(),
                null));
        user.recordLogin();
        socialAccountRepository.saveAndFlush(SocialAccount.create(
                user, AuthProvider.KAKAO, providerUserId, email, true));
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
