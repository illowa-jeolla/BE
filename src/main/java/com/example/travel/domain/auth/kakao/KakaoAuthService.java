package com.example.travel.domain.auth.kakao;

import com.example.travel.domain.auth.AuthService;
import com.example.travel.domain.auth.dto.AuthTokenResponse;
import com.example.travel.domain.auth.kakao.dto.KakaoUserResponse;
import com.example.travel.domain.user.AuthProvider;
import com.example.travel.domain.user.SocialAccountRepository;
import com.example.travel.global.common.ApiException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
public class KakaoAuthService {
    private final KakaoProperties properties;
    private final KakaoApiClient kakaoApiClient;
    private final KakaoLoginStateService stateService;
    private final KakaoUserWriter userWriter;
    private final SocialAccountRepository socialAccountRepository;
    private final AuthService authService;

    public KakaoAuthService(KakaoProperties properties, KakaoApiClient kakaoApiClient,
                            KakaoLoginStateService stateService, KakaoUserWriter userWriter,
                            SocialAccountRepository socialAccountRepository, AuthService authService) {
        this.properties = properties;
        this.kakaoApiClient = kakaoApiClient;
        this.stateService = stateService;
        this.userWriter = userWriter;
        this.socialAccountRepository = socialAccountRepository;
        this.authService = authService;
    }

    public URI authorizationUri(HttpServletResponse response) {
        String state = stateService.issue(response);
        return UriComponentsBuilder.fromUriString("https://kauth.kakao.com/oauth/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", properties.clientId())
                .queryParam("redirect_uri", properties.redirectUri())
                .queryParam("state", state)
                .build().encode().toUri();
    }

    public AuthTokenResponse callback(String code, String state, String cookieState, String error,
                                      HttpServletResponse response) {
        stateService.consume(state, cookieState, response);
        if (error != null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "KAKAO_401_LOGIN_CANCELLED",
                    "카카오 로그인이 취소되었습니다.");
        }
        if (code == null || code.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "KAKAO_400_MISSING_CODE",
                    "카카오 인가 코드가 없습니다.");
        }

        String kakaoAccessToken = kakaoApiClient.exchangeCode(code).accessToken();
        KakaoUserResponse kakaoUser = kakaoApiClient.getUser(kakaoAccessToken);
        Long userId = findOrCreateUser(kakaoUser);
        return authService.issueTokens(userId, response);
    }

    private Long findOrCreateUser(KakaoUserResponse kakaoUser) {
        try {
            return userWriter.findOrCreate(kakaoUser);
        } catch (DataIntegrityViolationException exception) {
            return socialAccountRepository
                    .findByProviderAndProviderUserId(AuthProvider.KAKAO, kakaoUser.id().toString())
                    .map(account -> account.getUser().getId())
                    .orElseThrow(() -> exception);
        }
    }
}
