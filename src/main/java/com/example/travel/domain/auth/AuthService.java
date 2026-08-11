package com.example.travel.domain.auth;

import com.example.travel.domain.auth.dto.AuthTokenResponse;
import com.example.travel.domain.auth.dto.LoginRequest;
import com.example.travel.domain.auth.dto.SignupRequest;
import com.example.travel.domain.user.User;
import com.example.travel.domain.user.UserRepository;
import com.example.travel.global.auth.JwtProvider;
import com.example.travel.global.auth.RefreshTokenCookieProvider;
import com.example.travel.global.auth.RefreshTokenService;
import com.example.travel.global.common.ApiException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenCookieProvider cookieProvider;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtProvider jwtProvider,
                       RefreshTokenService refreshTokenService, RefreshTokenCookieProvider cookieProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.refreshTokenService = refreshTokenService;
        this.cookieProvider = cookieProvider;
    }

    @Transactional
    public AuthTokenResponse signup(SignupRequest request, HttpServletResponse response) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "AUTH_409_DUPLICATE_EMAIL", "이미 사용 중인 이메일입니다.");
        }
        User user = userRepository.save(User.create(request.email(),
                passwordEncoder.encode(request.password()), request.nickname()));
        return issueTokens(user, response);
    }

    @Transactional(readOnly = true)
    public AuthTokenResponse login(LoginRequest request, HttpServletResponse response) {
        User user = userRepository.findByEmailAndDeletedFalse(request.email())
                .orElseThrow(this::invalidCredentials);
        if (!passwordEncoder.matches(request.password(), user.getPassword())) throw invalidCredentials();
        return issueTokens(user, response);
    }

    @Transactional(readOnly = true)
    public AuthTokenResponse refresh(String refreshToken, HttpServletResponse response) {
        if (!jwtProvider.isValidRefreshToken(refreshToken)) throw invalidToken();
        Long userId = jwtProvider.userId(refreshToken);
        if (!refreshTokenService.matches(userId, refreshToken)) throw invalidToken();
        User user = userRepository.findByIdAndDeletedFalse(userId).orElseThrow(this::invalidToken);
        return issueTokens(user, response);
    }

    public void logout(Long userId, HttpServletResponse response) {
        refreshTokenService.delete(userId);
        response.addHeader(HttpHeaders.SET_COOKIE, cookieProvider.expire().toString());
    }

    private AuthTokenResponse issueTokens(User user, HttpServletResponse response) {
        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());
        refreshTokenService.save(user.getId(), refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, cookieProvider.create(refreshToken).toString());
        return AuthTokenResponse.bearer(accessToken);
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_401_INVALID_CREDENTIALS",
                "이메일 또는 비밀번호가 올바르지 않습니다.");
    }

    private ApiException invalidToken() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_401_INVALID_TOKEN", "유효하지 않은 토큰입니다.");
    }
}
