package com.example.travel.domain.community.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
@ConditionalOnProperty(name = "community.image.storage", havingValue = "local")
public class LocalImageWebConfig implements WebMvcConfigurer {
    private final LocalImageProperties properties;

    public LocalImageWebConfig(LocalImageProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(properties.directory()).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/local-images/**").addResourceLocations(location);
    }
}
