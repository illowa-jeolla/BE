package com.example.travel.domain.auth;

import com.example.travel.domain.user.User;
import com.example.travel.domain.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserSignupWriter {
    private final UserRepository userRepository;

    public UserSignupWriter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User save(User user) {
        return userRepository.saveAndFlush(user);
    }
}
