package com.example.travel.domain.user.service;

import com.example.travel.domain.user.dto.UpdateNicknameRequest;
import com.example.travel.domain.user.entity.LocalCredential;
import com.example.travel.domain.user.entity.User;
import com.example.travel.domain.user.enums.UserStatus;
import com.example.travel.domain.user.exception.UserException;
import com.example.travel.domain.user.repository.UserRepository;
import com.example.travel.domain.user.repository.LocalCredentialRepository;
import com.example.travel.domain.user.repository.SocialAccountRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserProfileServiceTest {
    @Test
    void returnsCurrentUsersProfile() {
        UserRepository repository = mock(UserRepository.class);
        User user = mock(User.class);
        when(user.getId()).thenReturn(7L);
        when(user.getNickname()).thenReturn("여행자");
        when(repository.findByIdAndStatus(7L, UserStatus.ACTIVE)).thenReturn(Optional.of(user));
        LocalCredentialRepository credentialRepository = mock(LocalCredentialRepository.class);
        LocalCredential credential = mock(LocalCredential.class);
        when(credential.getEmail()).thenReturn("traveler@example.com");
        when(credentialRepository.findById(7L)).thenReturn(Optional.of(credential));
        UserProfileService service = new UserProfileService(repository, credentialRepository,
                mock(SocialAccountRepository.class));

        var response = service.findMine(7L);

        assertThat(response.userId()).isEqualTo(7L);
        assertThat(response.email()).isEqualTo("traveler@example.com");
        assertThat(response.nickname()).isEqualTo("여행자");
    }

    @Test
    void trimsAndUpdatesNickname() {
        UserRepository repository = mock(UserRepository.class);
        User user = mock(User.class);
        when(user.getNickname()).thenReturn("새 닉네임");
        when(repository.findByIdAndStatus(7L, UserStatus.ACTIVE)).thenReturn(Optional.of(user));
        UserProfileService service = service(repository);

        var response = service.updateNickname(7L, new UpdateNicknameRequest("  새 닉네임  "));

        verify(user).updateNickname("새 닉네임");
        assertThat(response.nickname()).isEqualTo("새 닉네임");
    }

    @Test
    void rejectsMissingOrInactiveUser() {
        UserRepository repository = mock(UserRepository.class);
        when(repository.findByIdAndStatus(7L, UserStatus.ACTIVE)).thenReturn(Optional.empty());
        UserProfileService service = service(repository);

        assertThatThrownBy(() -> service.findMine(7L))
                .isInstanceOfSatisfying(UserException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("USER_404_NOT_FOUND"));
    }

    @Test
    void rejectsNicknameUsedByAnotherUser() {
        UserRepository repository = mock(UserRepository.class);
        User user = mock(User.class);
        when(repository.findByIdAndStatus(7L, UserStatus.ACTIVE)).thenReturn(Optional.of(user));
        when(repository.existsByNicknameAndIdNot("중복 닉네임", 7L)).thenReturn(true);
        UserProfileService service = service(repository);

        assertThatThrownBy(() -> service.updateNickname(
                7L, new UpdateNicknameRequest(" 중복 닉네임 ")))
                .isInstanceOfSatisfying(UserException.class, exception ->
                        assertThat(exception.getCode())
                                .isEqualTo("USER_409_NICKNAME_ALREADY_EXISTS"));
        verify(user, org.mockito.Mockito.never()).updateNickname(org.mockito.ArgumentMatchers.anyString());
    }

    private UserProfileService service(UserRepository repository) {
        return new UserProfileService(repository, mock(LocalCredentialRepository.class),
                mock(SocialAccountRepository.class));
    }
}
