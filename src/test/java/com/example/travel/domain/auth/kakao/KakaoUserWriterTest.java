package com.example.travel.domain.auth.kakao;

import com.example.travel.domain.auth.kakao.dto.KakaoUserResponse;
import com.example.travel.domain.auth.kakao.service.KakaoUserWriter;
import com.example.travel.domain.user.repository.SocialAccountRepository;
import com.example.travel.domain.user.enums.AuthProvider;
import com.example.travel.domain.user.entity.LocalCredential;
import com.example.travel.domain.user.repository.LocalCredentialRepository;
import com.example.travel.domain.user.entity.SocialAccount;
import com.example.travel.domain.user.entity.User;
import com.example.travel.domain.user.repository.UserRepository;
import com.example.travel.domain.auth.kakao.exception.KakaoException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:kakao-user;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class KakaoUserWriterTest {
    @Autowired
    private KakaoUserWriter userWriter;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Autowired
    private LocalCredentialRepository credentialRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void createsSocialUserWithEmailAndWithoutPassword() {
        var kakaoUser = new KakaoUserResponse(123L, new KakaoUserResponse.KakaoAccount(
                true, false, true, true, "user@example.com",
                new KakaoUserResponse.Profile("여행자", "https://example.com/kakao.jpg")));

        Long userId = userWriter.findOrCreate(kakaoUser);
        var socialAccount = socialAccountRepository
                .findByProviderAndProviderUserId(AuthProvider.KAKAO, "123")
                .orElseThrow();

        assertThat(socialAccount.getUser().getId()).isEqualTo(userId);
        assertThat(socialAccount.getUser().getNickname()).isNotBlank();
        assertThat(socialAccount.getUser().getAvatarUrl())
                .isEqualTo("https://example.com/kakao.jpg");
        assertThat(socialAccount.getProviderEmail()).isEqualTo("user@example.com");
        assertThat(socialAccount.isEmailVerified()).isTrue();
        assertThat(socialAccount.getProvider()).isEqualTo(AuthProvider.KAKAO);
        assertThat(credentialRepository.findById(userId)).isEmpty();
    }

    @Test
    void createsSeparateKakaoUserWhenLocalEmailIsTheSame() {
        User localUser = userRepository.save(User.create("일반사용자"));
        credentialRepository.save(LocalCredential.create(
                localUser, "same@example.com", "encoded-password"));
        var kakaoUser = new KakaoUserResponse(789L, new KakaoUserResponse.KakaoAccount(
                true, false, true, true, "same@example.com",
                new KakaoUserResponse.Profile("카카오사용자", null)));

        Long kakaoUserId = userWriter.findOrCreate(kakaoUser);

        assertThat(kakaoUserId).isNotEqualTo(localUser.getId());
        assertThat(socialAccountRepository
                .findByProviderAndProviderUserId(AuthProvider.KAKAO, "789"))
                .get().extracting(SocialAccount::getProviderEmail)
                .isEqualTo("same@example.com");
    }

    @Test
    void rejectsUnverifiedEmail() {
        var kakaoUser = new KakaoUserResponse(456L, new KakaoUserResponse.KakaoAccount(
                true, false, true, false, "user@example.com", null));

        assertThatThrownBy(() -> userWriter.findOrCreate(kakaoUser))
                .isInstanceOfSatisfying(KakaoException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("KAKAO_422_EMAIL_REQUIRED"));
    }

    @Test
    void updatesExistingKakaoUsersProfileImageOnLogin() {
        var firstLogin = new KakaoUserResponse(999L, new KakaoUserResponse.KakaoAccount(
                true, false, true, true, "user@example.com",
                new KakaoUserResponse.Profile("사용자", "https://example.com/old.jpg")));
        Long userId = userWriter.findOrCreate(firstLogin);

        var nextLogin = new KakaoUserResponse(999L, new KakaoUserResponse.KakaoAccount(
                true, false, true, true, "user@example.com",
                new KakaoUserResponse.Profile("사용자", "https://example.com/new.jpg")));
        userWriter.findOrCreate(nextLogin);

        assertThat(userRepository.findById(userId).orElseThrow().getAvatarUrl())
                .isEqualTo("https://example.com/new.jpg");
    }
}
