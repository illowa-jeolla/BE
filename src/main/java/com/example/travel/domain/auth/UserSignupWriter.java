package com.example.travel.domain.auth;

import com.example.travel.domain.user.LocalCredential;
import com.example.travel.domain.user.LocalCredentialRepository;
import com.example.travel.domain.user.User;
import com.example.travel.domain.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserSignupWriter {
    private final UserRepository userRepository;
    private final LocalCredentialRepository credentialRepository;

    public UserSignupWriter(UserRepository userRepository,
                            LocalCredentialRepository credentialRepository) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
    }

    @Transactional
    public User save(User user, String email, String passwordHash) {
        User savedUser = userRepository.save(user);
        credentialRepository.saveAndFlush(
                LocalCredential.create(savedUser, email, passwordHash));
        return savedUser;
    }
}
