package com.example.travel.domain.user.repository;

import com.example.travel.domain.user.enums.UserStatus;
import com.example.travel.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByIdAndStatus(Long id, UserStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id and u.status = :status")
    Optional<User> findByIdAndStatusForUpdate(@Param("id") Long id,
                                               @Param("status") UserStatus status);
}
