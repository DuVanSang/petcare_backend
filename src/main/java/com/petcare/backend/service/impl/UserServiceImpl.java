package com.petcare.backend.service.impl;

import com.petcare.backend.dto.user.request.ChangePasswordRequest;
import com.petcare.backend.dto.user.request.UpdateProfileRequest;
import com.petcare.backend.dto.user.request.UpdateUserPreferencesRequest;
import com.petcare.backend.dto.user.response.UserDeviceResponse;
import com.petcare.backend.dto.user.response.UserResponse;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.exception.ResourceNotFoundException;
import com.petcare.backend.model.User;
import com.petcare.backend.repository.RefreshTokenRepository;
import com.petcare.backend.repository.UserRepository;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.UserService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse getCurrentUser(UserPrincipal principal) {
        return UserResponse.from(getUserOrThrow(principal.getId()));
    }

    @Override
    @Transactional
    public UserResponse updateProfile(UserPrincipal principal, UpdateProfileRequest request) {
        User user = getUserOrThrow(principal.getId());

        if (StringUtils.hasText(request.getFullName())) {
            user.setFullName(request.getFullName().trim());
        }

        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(emptyToNull(request.getPhoneNumber()));
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(emptyToNull(request.getAvatarUrl()));
        }

        return UserResponse.from(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse updatePreferences(UserPrincipal principal, UpdateUserPreferencesRequest request) {
        User user = getUserOrThrow(principal.getId());

        if (StringUtils.hasText(request.getLanguageCode())) {
            user.setLanguageCode(request.getLanguageCode().trim());
        }

        if (StringUtils.hasText(request.getTimezone())) {
            user.setTimezone(request.getTimezone().trim());
        }

        if (request.getPushNotificationEnabled() != null) {
            user.setPushNotificationEnabled(request.getPushNotificationEnabled());
        }

        return UserResponse.from(userRepository.save(user));
    }

    @Override
    @Transactional
    public void changePassword(UserPrincipal principal, ChangePasswordRequest request) {
        User user = getUserOrThrow(principal.getId());

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Mật khẩu hiện tại không chính xác");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Mật khẩu mới không được trùng với mật khẩu hiện tại");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    public List<UserDeviceResponse> getMyDevices(UserPrincipal principal) {
        return refreshTokenRepository.findByUserIdAndIsRevokedFalseAndExpiresAtAfter(
                principal.getId(), 
                LocalDateTime.now()
        )
        .stream()
        .map(UserDeviceResponse::from)
        .toList();
    }

    @Override
    @Transactional
    public void deleteMyDevice(UserPrincipal principal, Long deviceId) {
        refreshTokenRepository.findByIdAndUserId(deviceId, principal.getId())
                .ifPresent(token -> {
                    token.setIsRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
    }

    private String emptyToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
