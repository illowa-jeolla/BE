package com.example.travel.domain.auth.google.service;

import com.example.travel.domain.auth.dto.AuthTokenResponse;
import com.example.travel.domain.auth.google.client.GoogleApiClient;
import com.example.travel.domain.auth.google.config.GoogleProperties;
import com.example.travel.domain.auth.google.dto.GoogleUserInfo;
import com.example.travel.domain.auth.service.AuthService;
import com.example.travel.domain.user.enums.AuthProvider;
import com.example.travel.domain.user.repository.SocialAccountRepository;
import com.example.travel.global.common.ApiException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
public class GoogleAuthService {
    private final GoogleProperties properties;
    private final GoogleApiClient googleApiClient;
    private final GoogleLoginStateService stateService;
    private final GoogleUserWriter userWriter;
    private final SocialAccountRepository socialAccountRepository;
    private final AuthService authService;

    public GoogleAuthService(GoogleProperties properties, GoogleApiClient googleApiClient,
                             GoogleLoginStateService stateService, GoogleUserWriter userWriter,
                             SocialAccountRepository socialAccountRepository, AuthService authService) {
        this.properties = properties;
        this.googleApiClient = googleApiClient;
        this.stateService = stateService;
        this.userWriter = userWriter;
        this.socialAccountRepository = socialAccountRepository;
        this.authService = authService;
    }

    public URI authorizationUri(HttpServletResponse response) {
        String state = stateService.issue(response);
        return UriComponentsBuilder.fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("response_type", "code")
                .queryParam("client_id", properties.clientId())
                .queryParam("redirect_uri", properties.redirectUri())
                .queryParam("scope", "openid email profile")
                .queryParam("state", state)
                .build().encode().toUri();
    }

    public AuthTokenResponse callback(String code, String state, String cookieState, String error,
                                      HttpServletResponse response) {
        stateService.consume(state, cookieState, response);
        if (error != null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "GOOGLE_401_LOGIN_CANCELLED",
                    "Google 로그인이 취소되었습니다.");
        }
        if (code == null || code.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "GOOGLE_400_MISSING_CODE",
                    "Google 인가 코드가 없습니다.");
        }

        String idToken = googleApiClient.exchangeCode(code);
        GoogleUserInfo googleUser = googleApiClient.verifyIdToken(idToken);
        Long userId = findOrCreateUser(googleUser);
        return authService.issueTokens(userId, response);
    }

    private Long findOrCreateUser(GoogleUserInfo googleUser) {
        try {
            return userWriter.findOrCreate(googleUser);
        } catch (DataIntegrityViolationException exception) {
            return socialAccountRepository
                    .findByProviderAndProviderUserId(AuthProvider.GOOGLE, googleUser.subject())
                    .map(account -> account.getUser().getId())
                    .orElseThrow(() -> exception);
        }
    }
}
