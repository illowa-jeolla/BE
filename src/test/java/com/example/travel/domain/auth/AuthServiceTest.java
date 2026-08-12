package com.example.travel.domain.auth;

import com.example.travel.domain.auth.dto.LoginRequest;
import com.example.travel.domain.auth.dto.SignupRequest;
import com.example.travel.domain.auth.service.AuthService;
import com.example.travel.domain.auth.service.UserSignupWriter;
import com.example.travel.domain.user.entity.LocalCredential;
import com.example.travel.domain.user.repository.LocalCredentialRepository;
import com.example.travel.domain.user.entity.User;
import com.example.travel.domain.user.repository.UserRepository;
import com.example.travel.domain.user.enums.UserStatus;
import com.example.travel.global.auth.JwtProvider;
import com.example.travel.global.auth.RefreshTokenCookieProvider;
import com.example.travel.global.auth.RefreshTokenService;
import com.example.travel.global.common.ApiException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {
    private UserRepository userRepository;
    private LocalCredentialRepository credentialRepository;
    private PasswordEncoder passwordEncoder;
    private UserSignupWriter userSignupWriter;
    private JwtProvider jwtProvider;
    private RefreshTokenService refreshTokenService;
    private RefreshTokenCookieProvider cookieProvider;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        credentialRepository = mock(LocalCredentialRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        userSignupWriter = mock(UserSignupWriter.class);
        jwtProvider = mock(JwtProvider.class);
        refreshTokenService = mock(RefreshTokenService.class);
        cookieProvider = mock(RefreshTokenCookieProvider.class);
        authService = new AuthService(userRepository, credentialRepository, passwordEncoder,
                userSignupWriter, jwtProvider, refreshTokenService, cookieProvider);
    }

    @Test
    void rejectsDuplicateEmailBeforeSaving() {
        SignupRequest request = signupRequest();
        when(credentialRepository.existsByEmail(request.email())).thenReturn(true);

        assertDuplicateEmail(() -> authService.signup(request, mock(HttpServletResponse.class)));
    }

    @Test
    void convertsConcurrentEmailUniqueViolationToConflict() {
        SignupRequest request = signupRequest();
        when(credentialRepository.existsByEmail(request.email())).thenReturn(false, true);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(userSignupWriter.save(any(), anyString(), anyString()))
                .thenThrow(new DataIntegrityViolationException("unique violation"));

        assertDuplicateEmail(() -> authService.signup(request, mock(HttpServletResponse.class)));
    }

    @Test
    void doesNotConvertUnrelatedIntegrityViolation() {
        SignupRequest request = signupRequest();
        DataIntegrityViolationException exception = new DataIntegrityViolationException("other constraint");
        when(credentialRepository.existsByEmail(request.email())).thenReturn(false, false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(userSignupWriter.save(any(), anyString(), anyString())).thenThrow(exception);

        assertThatThrownBy(() -> authService.signup(request, mock(HttpServletResponse.class)))
                .isSameAs(exception);
    }

    @Test
    void rejectsConcurrentRefreshReuseBeforeIssuingAccessTokenOrCookie() {
        String currentToken = "current-refresh-token";
        String newToken = "new-refresh-token";
        User user = mock(User.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(jwtProvider.isValidRefreshToken(currentToken)).thenReturn(true);
        when(jwtProvider.userId(currentToken)).thenReturn(7L);
        when(userRepository.findByIdAndStatus(7L, UserStatus.ACTIVE)).thenReturn(Optional.of(user));
        when(jwtProvider.createRefreshToken(7L)).thenReturn(newToken);
        when(refreshTokenService.rotate(7L, currentToken, newToken)).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh(currentToken, response))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("AUTH_401_INVALID_TOKEN"));
        verify(jwtProvider, never()).createAccessToken(any(), any());
        verify(cookieProvider, never()).create(any());
        verify(response, never()).addHeader(any(), any());
    }

    @Test
    void socialUserCannotUsePasswordLoginWithoutLocalCredential() {
        when(credentialRepository.findByEmailAndUserStatus(
                "user@example.com", UserStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("user@example.com", "password123"),
                mock(HttpServletResponse.class)))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("AUTH_401_INVALID_CREDENTIALS"));
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void passwordLoginUsesLocalCredentialHash() {
        User user = mock(User.class);
        LocalCredential credential = mock(LocalCredential.class);
        when(credentialRepository.findByEmailAndUserStatus(
                "user@example.com", UserStatus.ACTIVE)).thenReturn(Optional.of(credential));
        when(credential.getPasswordHash()).thenReturn("encoded-password");
        when(credential.getUser()).thenReturn(user);
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("user@example.com", "password123"),
                mock(HttpServletResponse.class)))
                .isInstanceOf(ApiException.class);
    }

    private void assertDuplicateEmail(ThrowingCall call) {
        assertThatThrownBy(call::invoke)
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getCode()).isEqualTo("AUTH_409_DUPLICATE_EMAIL");
                });
    }

    private SignupRequest signupRequest() {
        return new SignupRequest("user@example.com", "password123", "traveler");
    }

    @FunctionalInterface
    private interface ThrowingCall {
        void invoke();
    }
}
