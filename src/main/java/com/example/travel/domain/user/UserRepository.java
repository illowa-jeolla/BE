package com.example.travel.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailAndDeletedFalse(String email);
    Optional<User> findByIdAndDeletedFalse(Long id);
    boolean existsByEmail(String email);
}
