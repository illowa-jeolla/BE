package com.example.travel.global.auth;

import com.example.travel.global.exception.BusinessException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

public final class OAuthCallbackRedirectUri {
    private OAuthCallbackRedirectUri() {}

    public static URI error(URI callbackUri, String providerError, String providerErrorDescription,
                            String state, BusinessException exception) {
        String error = hasText(providerError) ? providerError : standardError(exception.getCode());
        String description = hasText(providerErrorDescription)
                ? providerErrorDescription : exception.getCode();

        UriComponentsBuilder builder = UriComponentsBuilder.fromUri(callbackUri)
                .queryParam("error", error)
                .queryParam("error_description", description);
        if (hasText(state)) {
            builder.queryParam("state", state);
        }
        return builder.build().encode().toUri();
    }

    private static String standardError(String code) {
        if (code.endsWith("_MISSING_CODE") || code.endsWith("_INVALID_STATE")) {
            return "invalid_request";
        }
        if (code.endsWith("_LOGIN_CANCELLED") || code.endsWith("_EMAIL_REQUIRED")) {
            return "access_denied";
        }
        return "server_error";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
