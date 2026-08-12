package com.example.travel.domain.auth.kakao.client;

import com.example.travel.domain.auth.kakao.config.KakaoProperties;
import com.example.travel.domain.auth.kakao.dto.KakaoTokenResponse;
import com.example.travel.domain.auth.kakao.dto.KakaoUserResponse;
import com.example.travel.global.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class KakaoApiClient {
    private final KakaoProperties properties;
    private final RestClient restClient;

    public KakaoApiClient(KakaoProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
    }

    public KakaoTokenResponse exchangeCode(String code) {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        form.add("redirect_uri", properties.redirectUri());
        form.add("code", code);

        try {
            KakaoTokenResponse token = restClient.post()
                    .uri("https://kauth.kakao.com/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(KakaoTokenResponse.class);
            if (token == null || token.accessToken() == null) throw kakaoUnavailable();
            return token;
        } catch (RestClientException exception) {
            throw kakaoUnavailable();
        }
    }

    public KakaoUserResponse getUser(String accessToken) {
        try {
            KakaoUserResponse user = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("kapi.kakao.com")
                            .path("/v2/user/me")
                            .queryParam("property_keys",
                                    "[\"kakao_account.email\",\"kakao_account.profile\"]")
                            .build())
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(KakaoUserResponse.class);
            if (user == null || user.id() == null) throw kakaoUnavailable();
            return user;
        } catch (RestClientException exception) {
            throw kakaoUnavailable();
        }
    }

    private ApiException kakaoUnavailable() {
        return new ApiException(HttpStatus.BAD_GATEWAY, "KAKAO_502_API_ERROR",
                "카카오 로그인 처리 중 오류가 발생했습니다.");
    }
}
