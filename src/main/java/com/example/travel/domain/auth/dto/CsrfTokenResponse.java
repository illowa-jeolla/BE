package com.example.travel.domain.auth.dto;

public record CsrfTokenResponse(String token, String cookieName, String headerName) {}
