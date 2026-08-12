package com.example.travel.domain.user.repository;

import com.example.travel.domain.user.enums.AuthProvider;
import com.example.travel.domain.user.entity.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {
    Optional<SocialAccount> findByProviderAndProviderUserId(
            AuthProvider provider, String providerUserId);
}
