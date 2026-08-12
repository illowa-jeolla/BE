package com.example.travel.domain.auth.service;

import com.example.travel.domain.user.entity.LocalCredential;
import com.example.travel.domain.user.repository.LocalCredentialRepository;
import com.example.travel.domain.user.entity.User;
import com.example.travel.domain.user.repository.UserRepository;
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
