package com.example.travel.domain.user.repository;

import com.example.travel.domain.user.enums.UserStatus;
import com.example.travel.domain.user.entity.LocalCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocalCredentialRepository extends JpaRepository<LocalCredential, Long> {
    Optional<LocalCredential> findByEmailAndUserStatus(String email, UserStatus status);

    boolean existsByEmail(String email);
}
