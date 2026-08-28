package com.example.travel.domain.auth.google.controller;

import com.example.travel.domain.auth.google.service.GoogleAuthService;
import com.example.travel.domain.auth.google.service.GoogleLoginStateService;
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
@RequestMapping("/api/v1/auth/google")
public class GoogleAuthController {
    private final GoogleAuthService googleAuthService;
    private final FrontendProperties frontendProperties;

    public GoogleAuthController(GoogleAuthService googleAuthService,
                                FrontendProperties frontendProperties) {
        this.googleAuthService = googleAuthService;
        this.frontendProperties = frontendProperties;
    }

    @GetMapping
    public ResponseEntity<Void> login(HttpServletResponse response) {
        return ResponseEntity.status(302)
                .location(googleAuthService.authorizationUri(response))
                .build();
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription,
            @CookieValue(name = GoogleLoginStateService.COOKIE_NAME, required = false) String cookieState,
            HttpServletResponse response) {
        try {
            googleAuthService.callback(code, state, cookieState, error, response);
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
