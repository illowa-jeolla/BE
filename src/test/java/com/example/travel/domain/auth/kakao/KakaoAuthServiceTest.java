package com.example.travel.domain.auth.kakao;

import com.example.travel.domain.auth.AuthService;
import com.example.travel.domain.auth.dto.AuthTokenResponse;
import com.example.travel.domain.auth.kakao.dto.KakaoTokenResponse;
import com.example.travel.domain.auth.kakao.dto.KakaoUserResponse;
import com.example.travel.domain.user.SocialAccountRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KakaoAuthServiceTest {
    private KakaoApiClient apiClient;
    private KakaoLoginStateService stateService;
    private KakaoUserWriter userWriter;
    private AuthService authService;
    private KakaoAuthService service;

    @BeforeEach
    void setUp() {
        apiClient = mock(KakaoApiClient.class);
        stateService = mock(KakaoLoginStateService.class);
        userWriter = mock(KakaoUserWriter.class);
        authService = mock(AuthService.class);
        service = new KakaoAuthService(
                new KakaoProperties("client-id", "client-secret",
                        "http://localhost:8080/api/v1/auth/kakao/callback"),
                apiClient, stateService, userWriter,
                mock(SocialAccountRepository.class), authService);
    }

    @Test
    void authorizationUriContainsStateButNotClientSecret() {
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(stateService.issue(response)).thenReturn("oauth-state");

        String uri = service.authorizationUri(response).toString();

        assertThat(uri)
                .startsWith("https://kauth.kakao.com/oauth/authorize?")
                .contains("client_id=client-id", "state=oauth-state", "response_type=code")
                .doesNotContain("client-secret");
    }

    @Test
    void callbackReturnsServiceTokensDirectly() {
        HttpServletResponse response = mock(HttpServletResponse.class);
        KakaoUserResponse user = kakaoUser("user@example.com", true);
        AuthTokenResponse tokens = AuthTokenResponse.bearer("service-access-token");
        when(apiClient.exchangeCode("authorization-code"))
                .thenReturn(new KakaoTokenResponse("kakao-access-token"));
        when(apiClient.getUser("kakao-access-token")).thenReturn(user);
        when(userWriter.findOrCreate(user)).thenReturn(7L);
        when(authService.issueTokens(7L, response)).thenReturn(tokens);

        AuthTokenResponse result = service.callback(
                "authorization-code", "state", "state", null, response);

        verify(stateService).consume("state", "state", response);
        assertThat(result).isSameAs(tokens);
    }

    private KakaoUserResponse kakaoUser(String email, boolean verified) {
        return new KakaoUserResponse(123L, new KakaoUserResponse.KakaoAccount(
                true, false, true, verified, email,
                new KakaoUserResponse.Profile("여행자")));
    }
}
