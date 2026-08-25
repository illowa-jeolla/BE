package com.example.travel.global.config;

import com.example.travel.global.auth.JwtProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:security;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.locations=classpath:db/test-migration",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class SecurityConfigTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FilterChainProxy filterChainProxy;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    void removedTemporaryHomeIsNotPublic() throws Exception {
        mockMvc.perform(get("/home"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void errorDispatchIsNotMaskedAsUnauthorized() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/error")
                        .with(request -> {
                            request.setDispatcherType(jakarta.servlet.DispatcherType.ERROR);
                            return request;
                        }))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void usesCookieCsrfTokenRepository() {
        CsrfFilter csrfFilter = filterChainProxy.getFilters("/api/v1/auth/csrf").stream()
                .filter(CsrfFilter.class::isInstance)
                .map(CsrfFilter.class::cast)
                .findFirst()
                .orElseThrow();

        assertThat(ReflectionTestUtils.getField(csrfFilter, "tokenRepository"))
                .isInstanceOf(CookieCsrfTokenRepository.class);
    }

    @Test
    void csrfEndpointIssuesTokenAndCookie() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(jsonPath("$.data.cookieName").value("XSRF-TOKEN"))
                .andExpect(jsonPath("$.data.headerName").value("X-XSRF-TOKEN"));
    }

    @Test
    void refreshRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isForbidden());
    }

    @Test
    void validCsrfTokenPassesSecurityCheck() throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(csrfResult.getResponse().getCookie("XSRF-TOKEN"))
                        .header("X-XSRF-TOKEN",
                                csrfResult.getResponse().getCookie("XSRF-TOKEN").getValue()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tourApiRequiresAccessToken() throws Exception {
        mockMvc.perform(get("/api/v1/tour/places")
                        .param("region", "전주"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validAccessTokenPassesTourSecurityCheck() throws Exception {
        String token = jwtProvider.createAccessToken(7L, "USER");

        mockMvc.perform(get("/api/v1/tour/security-check")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void externalJobApiRequiresAccessToken() throws Exception {
        mockMvc.perform(get("/api/v1/jobs/external/tour"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/jobs/external/junnam"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validAccessTokenPassesExternalJobSecurityCheck() throws Exception {
        String token = jwtProvider.createAccessToken(7L, "USER");

        mockMvc.perform(get("/api/v1/jobs/external/security-check")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void corsPreflightAllowsGetAndPostWithConfiguredHeaders() throws Exception {
        mockMvc.perform(options("/api/v1/tour/places")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers",
                                "Content-Type, Authorization, X-XSRF-TOKEN"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("GET")))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("Content-Type")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("Authorization")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("X-XSRF-TOKEN")));
    }
}
