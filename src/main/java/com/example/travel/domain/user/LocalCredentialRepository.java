package com.example.travel.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocalCredentialRepository extends JpaRepository<LocalCredential, Long> {
    Optional<LocalCredential> findByEmailAndUserStatus(String email, UserStatus status);

    boolean existsByEmail(String email);
}
