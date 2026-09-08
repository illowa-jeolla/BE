package com.example.travel.domain.user;

import com.example.travel.domain.user.entity.User;
import com.example.travel.domain.user.repository.UserRepository;
import com.example.travel.global.auth.JwtProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:profile-nickname;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
class UserProfileNicknameMvcTest {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired JwtProvider jwtProvider;

    @Test
    void acceptsTenCharacterNicknameSurroundedBySpaces() throws Exception {
        User user = userRepository.saveAndFlush(User.create("before-one"));

        mockMvc.perform(patch("/api/v1/users/me/nickname")
                        .header("Authorization", "Bearer " + accessToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"  1234567890  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("1234567890"));
    }

    @Test
    void rejectsNicknameLongerThanTenCharactersAfterTrim() throws Exception {
        User user = userRepository.saveAndFlush(User.create("before-two"));

        mockMvc.perform(patch("/api/v1/users/me/nickname")
                        .header("Authorization", "Bearer " + accessToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"  12345678901  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_VALIDATION"));
    }

    private String accessToken(User user) {
        return jwtProvider.createAccessToken(user.getId(), user.getRole().name());
    }
}
