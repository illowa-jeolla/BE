package com.example.travel.domain.auth.service;

import com.example.travel.domain.auth.dto.AuthTokenResponse;
import com.example.travel.domain.auth.dto.LoginRequest;
import com.example.travel.domain.auth.dto.SignupRequest;
import com.example.travel.domain.auth.exception.AuthErrorCode;
import com.example.travel.domain.auth.exception.AuthException;
import com.example.travel.domain.user.entity.LocalCredential;
import com.example.travel.domain.user.repository.LocalCredentialRepository;
import com.example.travel.domain.user.entity.User;
import com.example.travel.domain.user.repository.UserRepository;
import com.example.travel.domain.user.enums.UserStatus;
import com.example.travel.global.auth.JwtProvider;
import com.example.travel.global.auth.RefreshTokenCookieProvider;
import com.example.travel.global.auth.RefreshTokenService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final LocalCredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserSignupWriter userSignupWriter;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenCookieProvider cookieProvider;

    public AuthService(UserRepository userRepository,
                       LocalCredentialRepository credentialRepository,
                       PasswordEncoder passwordEncoder,
                       UserSignupWriter userSignupWriter,
                       JwtProvider jwtProvider,
                       RefreshTokenService refreshTokenService,
                       RefreshTokenCookieProvider cookieProvider) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.userSignupWriter = userSignupWriter;
        this.jwtProvider = jwtProvider;
        this.refreshTokenService = refreshTokenService;
        this.cookieProvider = cookieProvider;
    }

    public AuthTokenResponse signup(SignupRequest request, HttpServletResponse response) {
        if (credentialRepository.existsByEmail(request.email())) throw duplicateEmail();

        User user;
        try {
            User newUser = User.create(request.nickname());
            newUser.recordLogin();
            user = userSignupWriter.save(
                    newUser,
                    request.email(),
                    passwordEncoder.encode(request.password()));
        } catch (DataIntegrityViolationException exception) {
            if (credentialRepository.existsByEmail(request.email())) throw duplicateEmail();
            throw exception;
        }
        return issueTokens(user, response);
    }

    @Transactional
    public AuthTokenResponse login(LoginRequest request, HttpServletResponse response) {
        LocalCredential credential = credentialRepository
                .findByEmailAndUserStatus(request.email(), UserStatus.ACTIVE)
                .orElseThrow(this::invalidCredentials);
        if (!passwordEncoder.matches(request.password(), credential.getPasswordHash())) {
            throw invalidCredentials();
        }
        credential.getUser().recordLogin();
        return issueTokens(credential.getUser(), response);
    }

    @Transactional(readOnly = true)
    public AuthTokenResponse refresh(String refreshToken, HttpServletResponse response) {
        if (!jwtProvider.isValidRefreshToken(refreshToken)) throw invalidToken();
        Long userId = jwtProvider.userId(refreshToken);
        User user = activeUser(userId);
        String newRefreshToken = jwtProvider.createRefreshToken(userId);
        if (!refreshTokenService.rotate(userId, refreshToken, newRefreshToken)) throw invalidToken();

        String accessToken = jwtProvider.createAccessToken(userId, user.getRole().name());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieProvider.create(newRefreshToken).toString());
        return AuthTokenResponse.bearer(accessToken);
    }

    public void logout(Long userId, HttpServletResponse response) {
        refreshTokenService.delete(userId);
        response.addHeader(HttpHeaders.SET_COOKIE, cookieProvider.expire().toString());
    }

    @Transactional(readOnly = true)
    public AuthTokenResponse issueTokens(Long userId, HttpServletResponse response) {
        return issueTokens(activeUser(userId), response);
    }

    private User activeUser(Long userId) {
        return userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)
                .orElseThrow(this::invalidToken);
    }

    private AuthTokenResponse issueTokens(User user, HttpServletResponse response) {
        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());
        refreshTokenService.save(user.getId(), refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, cookieProvider.create(refreshToken).toString());
        return AuthTokenResponse.bearer(accessToken);
    }

    private AuthException invalidCredentials() {
        return new AuthException(AuthErrorCode.INVALID_CREDENTIALS);
    }

    private AuthException duplicateEmail() {
        return new AuthException(AuthErrorCode.DUPLICATE_EMAIL);
    }

    private AuthException invalidToken() {
        return new AuthException(AuthErrorCode.INVALID_TOKEN);
    }
}
