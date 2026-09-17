package com.example.travel.domain.auth.controller;

import com.example.travel.domain.auth.service.AuthService;
import com.example.travel.domain.auth.dto.AuthTokenResponse;
import com.example.travel.domain.auth.dto.CsrfTokenResponse;
import com.example.travel.domain.auth.dto.LoginRequest;
import com.example.travel.domain.auth.dto.SignupRequest;
import com.example.travel.global.common.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> signup(
            @Valid @RequestBody SignupRequest request, HttpServletResponse response) {
        return ResponseEntity.ok(ApiResponse.success(authService.signup(request, response)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> login(
            @Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request, response)));
    }

    @GetMapping("/csrf")
    public ResponseEntity<ApiResponse<CsrfTokenResponse>> csrf(
            @RequestAttribute("_csrf") CsrfToken csrfToken) {
        csrfToken.getToken();
        return ResponseEntity.ok(ApiResponse.success(
                new CsrfTokenResponse(csrfToken.getToken(), "XSRF-TOKEN", csrfToken.getHeaderName())));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> refresh(
            @CookieValue("refresh_token") String refreshToken, HttpServletResponse response) {
        return ResponseEntity.ok(ApiResponse.success(authService.refresh(refreshToken, response)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(Authentication authentication, HttpServletResponse response) {
        authService.logout((Long) authentication.getPrincipal(), response);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
