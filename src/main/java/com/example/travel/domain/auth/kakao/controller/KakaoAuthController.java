package com.example.travel.domain.auth.kakao.controller;

import com.example.travel.domain.auth.kakao.service.KakaoAuthService;
import com.example.travel.domain.auth.kakao.service.KakaoLoginStateService;
import com.example.travel.global.auth.OAuthCallbackRedirectUri;
import com.example.travel.global.config.FrontendProperties;
import com.example.travel.global.exception.BusinessException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/kakao")
public class KakaoAuthController {
    private final KakaoAuthService kakaoAuthService;
    private final FrontendProperties frontendProperties;

    public KakaoAuthController(KakaoAuthService kakaoAuthService,
                               FrontendProperties frontendProperties) {
        this.kakaoAuthService = kakaoAuthService;
        this.frontendProperties = frontendProperties;
    }

    @GetMapping
    public ResponseEntity<Void> login(HttpServletResponse response) {
        return ResponseEntity.status(302)
                .location(kakaoAuthService.authorizationUri(response))
                .build();
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription,
            @CookieValue(name = KakaoLoginStateService.COOKIE_NAME, required = false) String cookieState,
            HttpServletResponse response) {
        try {
            kakaoAuthService.callback(code, state, cookieState, error, response);
        } catch (BusinessException exception) {
            return ResponseEntity.status(302)
                    .location(OAuthCallbackRedirectUri.error(frontendProperties.oauthCallbackUri(),
                            error, errorDescription, state, exception))
                    .build();
        }
        return ResponseEntity.status(302)
                .location(frontendProperties.oauthCallbackUri())
                .build();
    }
}
