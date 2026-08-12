package com.example.travel.domain.auth.google;

import com.example.travel.domain.auth.dto.AuthTokenResponse;
import com.example.travel.domain.auth.google.client.GoogleApiClient;
import com.example.travel.domain.auth.google.config.GoogleProperties;
import com.example.travel.domain.auth.google.dto.GoogleUserInfo;
import com.example.travel.domain.auth.google.service.GoogleAuthService;
import com.example.travel.domain.auth.google.service.GoogleLoginStateService;
import com.example.travel.domain.auth.google.service.GoogleUserWriter;
import com.example.travel.domain.auth.service.AuthService;
import com.example.travel.domain.user.repository.SocialAccountRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoogleAuthServiceTest {
    private GoogleApiClient apiClient;
    private GoogleLoginStateService stateService;
    private GoogleUserWriter userWriter;
    private AuthService authService;
    private GoogleAuthService service;

    @BeforeEach
    void setUp() {
        apiClient = mock(GoogleApiClient.class);
        stateService = mock(GoogleLoginStateService.class);
        userWriter = mock(GoogleUserWriter.class);
        authService = mock(AuthService.class);
        service = new GoogleAuthService(
                new GoogleProperties("client-id", "client-secret",
                        "http://localhost:8080/api/v1/auth/google/callback"),
                apiClient, stateService, userWriter,
                mock(SocialAccountRepository.class), authService);
    }

    @Test
    void authorizationUriContainsOpenIdScopesAndStateButNotClientSecret() {
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(stateService.issue(response)).thenReturn("oauth-state");

        String uri = service.authorizationUri(response).toString();

        assertThat(uri)
                .startsWith("https://accounts.google.com/o/oauth2/v2/auth?")
                .contains("client_id=client-id", "state=oauth-state", "response_type=code")
                .contains("scope=openid%20email%20profile")
                .doesNotContain("client-secret");
    }

    @Test
    void callbackVerifiesGoogleIdTokenAndReturnsServiceTokens() {
        HttpServletResponse response = mock(HttpServletResponse.class);
        GoogleUserInfo user = new GoogleUserInfo(
                "google-subject", "user@example.com", true, "여행자", null);
        AuthTokenResponse tokens = AuthTokenResponse.bearer("service-access-token");
        when(apiClient.exchangeCode("authorization-code")).thenReturn("google-id-token");
        when(apiClient.verifyIdToken("google-id-token")).thenReturn(user);
        when(userWriter.findOrCreate(user)).thenReturn(7L);
        when(authService.issueTokens(7L, response)).thenReturn(tokens);

        AuthTokenResponse result = service.callback(
                "authorization-code", "state", "state", null, response);

        verify(stateService).consume("state", "state", response);
        verify(apiClient).verifyIdToken("google-id-token");
        assertThat(result).isSameAs(tokens);
    }
}
