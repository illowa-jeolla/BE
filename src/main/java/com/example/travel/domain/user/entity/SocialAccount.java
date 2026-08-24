package com.example.travel.domain.user.entity;

import com.example.travel.domain.user.enums.AuthProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;

@Getter
@Entity
@Table(name = "user_oauth_accounts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_oauth_provider_user", columnNames = {"provider", "provider_user_id"}),
        @UniqueConstraint(name = "uk_oauth_user_provider", columnNames = {"user_id", "provider"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider provider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @Column(name = "provider_email", length = 255)
    private String providerEmail;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private OffsetDateTime updatedAt;

    private SocialAccount(User user, AuthProvider provider, String providerUserId,
                          String providerEmail, boolean emailVerified) {
        this.user = user;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.providerEmail = providerEmail;
        this.emailVerified = emailVerified;
    }

    public static SocialAccount create(User user, AuthProvider provider, String providerUserId,
                                       String providerEmail, boolean emailVerified) {
        return new SocialAccount(user, provider, providerUserId, providerEmail, emailVerified);
    }

    public void updateEmail(String providerEmail, boolean emailVerified) {
        this.providerEmail = providerEmail;
        this.emailVerified = emailVerified;
    }
}
