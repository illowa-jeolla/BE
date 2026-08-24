package com.example.travel.domain.auth.dto;

public record CsrfTokenResponse(String cookieName, String headerName) {}
