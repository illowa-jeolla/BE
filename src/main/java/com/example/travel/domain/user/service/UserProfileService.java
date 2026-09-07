package com.example.travel.domain.user.service;

import com.example.travel.domain.user.dto.MyProfileResponse;
import com.example.travel.domain.user.dto.UpdateNicknameRequest;
import com.example.travel.domain.user.entity.User;
import com.example.travel.domain.user.enums.UserStatus;
import com.example.travel.domain.user.exception.UserErrorCode;
import com.example.travel.domain.user.exception.UserException;
import com.example.travel.domain.user.repository.LocalCredentialRepository;
import com.example.travel.domain.user.repository.SocialAccountRepository;
import com.example.travel.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserProfileService {
    private final UserRepository userRepository;
    private final LocalCredentialRepository localCredentialRepository;
    private final SocialAccountRepository socialAccountRepository;

    public UserProfileService(UserRepository userRepository,
                              LocalCredentialRepository localCredentialRepository,
                              SocialAccountRepository socialAccountRepository) {
        this.userRepository = userRepository;
        this.localCredentialRepository = localCredentialRepository;
        this.socialAccountRepository = socialAccountRepository;
    }

    public MyProfileResponse findMine(Long userId) {
        return response(findActiveUser(userId));
    }

    @Transactional
    public MyProfileResponse updateNickname(Long userId, UpdateNicknameRequest request) {
        User user = findActiveUser(userId);
        String nickname = request.nickname().trim();
        if (userRepository.existsByNicknameAndIdNot(nickname, userId)) {
            throw new UserException(UserErrorCode.NICKNAME_ALREADY_EXISTS);
        }
        user.updateNickname(nickname);
        return response(user);
    }

    private MyProfileResponse response(User user) {
        String email = localCredentialRepository.findById(user.getId())
                .map(credential -> credential.getEmail())
                .orElseGet(() -> socialAccountRepository
                        .findFirstByUserIdAndProviderEmailIsNotNullOrderByIdAsc(user.getId())
                        .map(account -> account.getProviderEmail())
                        .orElse(null));
        return MyProfileResponse.from(user, email);
    }

    private User findActiveUser(Long userId) {
        return userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND));
    }
}
