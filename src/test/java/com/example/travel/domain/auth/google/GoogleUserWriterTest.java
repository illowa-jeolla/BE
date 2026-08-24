package com.example.travel.domain.auth.google;

import com.example.travel.domain.auth.google.dto.GoogleUserInfo;
import com.example.travel.domain.auth.google.service.GoogleUserWriter;
import com.example.travel.domain.user.entity.LocalCredential;
import com.example.travel.domain.user.entity.SocialAccount;
import com.example.travel.domain.user.entity.User;
import com.example.travel.domain.user.enums.AuthProvider;
import com.example.travel.domain.user.repository.LocalCredentialRepository;
import com.example.travel.domain.user.repository.SocialAccountRepository;
import com.example.travel.domain.user.repository.UserRepository;
import com.example.travel.domain.auth.google.exception.GoogleException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:google-user;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.locations=classpath:db/test-migration",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class GoogleUserWriterTest {
    @Autowired
    private GoogleUserWriter userWriter;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Autowired
    private LocalCredentialRepository credentialRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void createsGoogleUserUsingSubjectAsProviderUserId() {
        var googleUser = new GoogleUserInfo(
                "google-subject", "USER@EXAMPLE.COM", true,
                "여행자", "https://example.com/profile.jpg");

        Long userId = userWriter.findOrCreate(googleUser);
        SocialAccount socialAccount = socialAccountRepository
                .findByProviderAndProviderUserId(AuthProvider.GOOGLE, "google-subject")
                .orElseThrow();

        assertThat(socialAccount.getUser().getId()).isEqualTo(userId);
        assertThat(socialAccount.getProviderEmail()).isEqualTo("user@example.com");
        assertThat(socialAccount.isEmailVerified()).isTrue();
        assertThat(socialAccount.getProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(socialAccount.getUser().getAvatarUrl())
                .isEqualTo("https://example.com/profile.jpg");
        assertThat(credentialRepository.findById(userId)).isEmpty();
    }

    @Test
    void createsSeparateGoogleUserWhenLocalEmailIsTheSame() {
        User localUser = userRepository.save(User.create("일반사용자"));
        credentialRepository.save(LocalCredential.create(
                localUser, "same@example.com", "encoded-password"));
        var googleUser = new GoogleUserInfo(
                "separate-subject", "same@example.com", true, "구글사용자", null);

        Long googleUserId = userWriter.findOrCreate(googleUser);

        assertThat(googleUserId).isNotEqualTo(localUser.getId());
        assertThat(socialAccountRepository
                .findByProviderAndProviderUserId(AuthProvider.GOOGLE, "separate-subject"))
                .get().extracting(SocialAccount::getProviderEmail)
                .isEqualTo("same@example.com");
    }

    @Test
    void rejectsUnverifiedEmail() {
        var googleUser = new GoogleUserInfo(
                "google-subject", "user@example.com", false, "사용자", null);

        assertThatThrownBy(() -> userWriter.findOrCreate(googleUser))
                .isInstanceOfSatisfying(GoogleException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("GOOGLE_422_EMAIL_REQUIRED"));
    }
}
