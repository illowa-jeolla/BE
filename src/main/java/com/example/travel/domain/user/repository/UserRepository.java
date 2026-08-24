package com.example.travel.domain.user.repository;

import com.example.travel.domain.user.enums.UserStatus;
import com.example.travel.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByIdAndStatus(Long id, UserStatus status);
}
