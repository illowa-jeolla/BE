package com.example.travel.domain.auth.google.client;

import com.example.travel.domain.auth.google.config.GoogleProperties;
import com.example.travel.domain.auth.google.dto.GoogleTokenResponse;
import com.example.travel.domain.auth.google.dto.GoogleUserInfo;
import com.example.travel.global.common.ApiException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@Component
public class GoogleApiClient {
    private final GoogleProperties properties;
    private final RestClient restClient;
    private final GoogleIdTokenVerifier idTokenVerifier;

    public GoogleApiClient(GoogleProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
        this.idTokenVerifier = createVerifier(properties.clientId());
    }

    public String exchangeCode(String code) {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        form.add("redirect_uri", properties.redirectUri());
        form.add("code", code);

        try {
            GoogleTokenResponse token = restClient.post()
                    .uri("https://oauth2.googleapis.com/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(GoogleTokenResponse.class);
            if (token == null || token.idToken() == null || token.idToken().isBlank()) {
                throw googleUnavailable();
            }
            return token.idToken();
        } catch (RestClientException exception) {
            throw googleUnavailable();
        }
    }

    public GoogleUserInfo verifyIdToken(String rawIdToken) {
        try {
            GoogleIdToken idToken = idTokenVerifier.verify(rawIdToken);
            if (idToken == null) throw invalidIdToken();

            GoogleIdToken.Payload payload = idToken.getPayload();
            return new GoogleUserInfo(
                    payload.getSubject(),
                    payload.getEmail(),
                    Boolean.TRUE.equals(payload.getEmailVerified()),
                    stringClaim(payload, "name"),
                    stringClaim(payload, "picture"));
        } catch (GeneralSecurityException | IOException | IllegalArgumentException exception) {
            throw invalidIdToken();
        }
    }

    private GoogleIdTokenVerifier createVerifier(String clientId) {
        try {
            return new GoogleIdTokenVerifier.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(List.of(clientId))
                    .build();
        } catch (GeneralSecurityException | IOException exception) {
            throw new IllegalStateException("Google ID Token verifier를 초기화할 수 없습니다.", exception);
        }
    }

    private String stringClaim(GoogleIdToken.Payload payload, String name) {
        Object value = payload.get(name);
        return value instanceof String string ? string : null;
    }

    private ApiException invalidIdToken() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "GOOGLE_401_INVALID_ID_TOKEN",
                "유효하지 않은 Google ID Token입니다.");
    }

    private ApiException googleUnavailable() {
        return new ApiException(HttpStatus.BAD_GATEWAY, "GOOGLE_502_API_ERROR",
                "Google 로그인 처리 중 오류가 발생했습니다.");
    }
}
