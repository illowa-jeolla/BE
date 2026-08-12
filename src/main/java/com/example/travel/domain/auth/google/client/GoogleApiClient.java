package com.example.travel.domain.auth.google.client;

import com.example.travel.domain.auth.google.config.GoogleProperties;
import com.example.travel.domain.auth.google.dto.GoogleTokenResponse;
import com.example.travel.domain.auth.google.dto.GoogleUserInfo;
import com.example.travel.global.common.ApiException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GooglePublicKeysManager;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.apache.v2.ApacheHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.apache.http.client.config.RequestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.List;

@Component
public class GoogleApiClient {
    private static final Logger log = LoggerFactory.getLogger(GoogleApiClient.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    private final GoogleProperties properties;
    private final RestClient restClient;
    private final HttpRequestFactory googleRequestFactory;
    private final GoogleIdTokenVerifier idTokenVerifier;

    public GoogleApiClient(GoogleProperties properties) {
        this.properties = properties;
        this.restClient = createRestClient();
        this.googleRequestFactory = createGoogleRequestFactory();
        this.idTokenVerifier = createVerifier(properties.clientId(), googleRequestFactory);
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
        } catch (RestClientResponseException exception) {
            String googleErrorCode = googleErrorCode(exception);
            log.warn("Google token endpoint rejected the request: status={}, errorCode={}",
                    exception.getStatusCode().value(), googleErrorCode);
            throw googleUnavailable(exception);
        } catch (RestClientException exception) {
            log.warn("Google token endpoint request failed: cause={}",
                    exception.getClass().getSimpleName());
            throw googleUnavailable(exception);
        }
    }

    public GoogleUserInfo verifyIdToken(String rawIdToken) {
        try {
            GoogleIdToken idToken = idTokenVerifier.verify(rawIdToken);
            if (idToken == null) {
                log.warn("Google ID Token verification returned no verified token");
                throw invalidIdToken(new GeneralSecurityException("ID Token verification failed"));
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            return new GoogleUserInfo(
                    payload.getSubject(),
                    payload.getEmail(),
                    Boolean.TRUE.equals(payload.getEmailVerified()),
                    stringClaim(payload, "name"),
                    stringClaim(payload, "picture"));
        } catch (IOException exception) {
            log.warn("Google public key retrieval failed: cause={}",
                    exception.getClass().getSimpleName());
            throw googleUnavailable(exception);
        } catch (GeneralSecurityException exception) {
            log.warn("Google ID Token security verification failed: cause={}",
                    exception.getClass().getSimpleName());
            throw invalidIdToken(exception);
        } catch (IllegalArgumentException exception) {
            log.warn("Google ID Token format validation failed: cause={}",
                    exception.getClass().getSimpleName());
            throw invalidIdToken(exception);
        }
    }

    private RestClient createRestClient() {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    private HttpRequestFactory createGoogleRequestFactory() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout((int) CONNECT_TIMEOUT.toMillis())
                .setConnectionRequestTimeout((int) CONNECT_TIMEOUT.toMillis())
                .setSocketTimeout((int) READ_TIMEOUT.toMillis())
                .build();
        var httpClient = ApacheHttpTransport.newDefaultHttpClientBuilder()
                .setDefaultRequestConfig(requestConfig)
                .build();
        var transport = new ApacheHttpTransport(httpClient);
        return transport.createRequestFactory(request -> {
            request.setConnectTimeout((int) CONNECT_TIMEOUT.toMillis());
            request.setReadTimeout((int) READ_TIMEOUT.toMillis());
        });
    }

    private GoogleIdTokenVerifier createVerifier(String clientId, HttpRequestFactory requestFactory) {
        var publicKeysManager = new GooglePublicKeysManager.Builder(
                requestFactory.getTransport(), GsonFactory.getDefaultInstance()).build();
        return new GoogleIdTokenVerifier.Builder(publicKeysManager)
                .setAudience(List.of(clientId))
                .build();
    }

    private String stringClaim(GoogleIdToken.Payload payload, String name) {
        Object value = payload.get(name);
        return value instanceof String string ? string : null;
    }

    private String googleErrorCode(RestClientResponseException exception) {
        try {
            GoogleErrorResponse error = exception.getResponseBodyAs(GoogleErrorResponse.class);
            return error == null || error.error() == null ? "unknown" : error.error();
        } catch (RuntimeException ignored) {
            return "unknown";
        }
    }

    private ApiException invalidIdToken(Throwable cause) {
        return new ApiException(HttpStatus.UNAUTHORIZED, "GOOGLE_401_INVALID_ID_TOKEN",
                "유효하지 않은 Google ID Token입니다.", cause);
    }

    private ApiException googleUnavailable() {
        return new ApiException(HttpStatus.BAD_GATEWAY, "GOOGLE_502_API_ERROR",
                "Google 로그인 처리 중 오류가 발생했습니다.");
    }

    private ApiException googleUnavailable(Throwable cause) {
        return new ApiException(HttpStatus.BAD_GATEWAY, "GOOGLE_502_API_ERROR",
                "Google 로그인 처리 중 오류가 발생했습니다.", cause);
    }

    private record GoogleErrorResponse(String error) {}
}
