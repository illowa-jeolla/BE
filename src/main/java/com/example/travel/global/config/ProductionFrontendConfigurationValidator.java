package com.example.travel.global.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
@Profile({"prod", "production"})
public class ProductionFrontendConfigurationValidator implements InitializingBean {
    private final FrontendProperties frontendProperties;

    public ProductionFrontendConfigurationValidator(FrontendProperties frontendProperties) {
        this.frontendProperties = frontendProperties;
    }

    @Override
    public void afterPropertiesSet() {
        rejectLocalhost(frontendProperties.oauthCallbackUri(), "FRONTEND_OAUTH_CALLBACK_URI");
        rejectLocalhost(URI.create(frontendProperties.origin()), "FRONTEND_ORIGIN");
    }

    private static void rejectLocalhost(URI uri, String propertyName) {
        if ("localhost".equalsIgnoreCase(uri.getHost())) {
            throw new IllegalStateException(propertyName + " must not use localhost in production");
        }
    }
}
