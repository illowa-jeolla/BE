package com.example.travel.domain.user;

import com.example.travel.domain.user.entity.User;
import com.example.travel.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:user-nickname-constraint;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class UserNicknameUniqueConstraintIntegrationTest {
    @Autowired UserRepository userRepository;

    @Test
    void databaseRejectsDuplicateNickname() {
        userRepository.saveAndFlush(User.create("같은닉네임"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(User.create("같은닉네임")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
