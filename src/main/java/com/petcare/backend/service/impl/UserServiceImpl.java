package com.petcare.backend.service.impl;

import com.petcare.backend.dto.auth.request.DeviceInfoRequest;
import com.petcare.backend.dto.user.request.ChangePasswordRequest;
import com.petcare.backend.dto.user.request.UpdateProfileRequest;
import com.petcare.backend.dto.user.request.UpdateUserPreferencesRequest;
import com.petcare.backend.dto.user.response.PasswordStatusResponse;
import com.petcare.backend.dto.user.response.UserDeviceResponse;
import com.petcare.backend.dto.user.response.UserResponse;
import com.petcare.backend.dto.upload.UploadFileResponse;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.exception.ResourceNotFoundException;
import com.petcare.backend.model.User;
import com.petcare.backend.model.UserDevice;
import com.petcare.backend.repository.UserDeviceRepository;
import com.petcare.backend.repository.UserRepository;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.FileStorageService;
import com.petcare.backend.service.UserService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    @Override
    public UserResponse getCurrentUser(UserPrincipal principal) {
        return UserResponse.from(getUserOrThrow(principal.getId()));
    }

    @Override
    public UserResponse getUserById(Long userId) {
        return UserResponse.from(getUserOrThrow(userId));
    }

    @Override
    @Transactional
    public UserResponse updateProfile(UserPrincipal principal, UpdateProfileRequest request) {
        User user = getUserOrThrow(principal.getId());

        if (StringUtils.hasText(request.getFullName())) {
            user.setFullName(request.getFullName().trim());
        }

        if (request.getUsername() != null) {
            String username = emptyToNull(request.getUsername());
            if (username != null && userRepository.existsByUsernameIgnoreCaseAndIdNot(username, user.getId())) {
                throw new BadRequestException("Username đã được sử dụng");
            }
            user.setUsername(username);
        }

        if (request.getBio() != null) {
            user.setBio(emptyToNull(request.getBio()));
        }

        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }

        if (request.getLocation() != null) {
            user.setLocation(emptyToNull(request.getLocation()));
        }

        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(emptyToNull(request.getPhoneNumber()));
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(emptyToNull(request.getAvatarUrl()));
        }

        if (request.getCoverImageUrl() != null) {
            user.setCoverImageUrl(emptyToNull(request.getCoverImageUrl()));
        }

        return UserResponse.from(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse uploadAvatar(UserPrincipal principal, MultipartFile file) {
        User user = getUserOrThrow(principal.getId());
        UploadFileResponse uploadedFile = fileStorageService.storeUserProfileImage(file, user.getId(), "avatar");
        String oldAvatarUrl = user.getAvatarUrl();
        user.setAvatarUrl(uploadedFile.getMediaUrl());
        User savedUser = userRepository.save(user);
        fileStorageService.deleteByUrl(oldAvatarUrl);
        return UserResponse.from(savedUser);
    }

    @Override
    @Transactional
    public UserResponse uploadCoverImage(UserPrincipal principal, MultipartFile file) {
        User user = getUserOrThrow(principal.getId());
        UploadFileResponse uploadedFile = fileStorageService.storeUserProfileImage(file, user.getId(), "cover");
        String oldCoverImageUrl = user.getCoverImageUrl();
        user.setCoverImageUrl(uploadedFile.getMediaUrl());
        User savedUser = userRepository.save(user);
        fileStorageService.deleteByUrl(oldCoverImageUrl);
        return UserResponse.from(savedUser);
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
    public PasswordStatusResponse getPasswordStatus(UserPrincipal principal) {
        User user = getUserOrThrow(principal.getId());
        return PasswordStatusResponse.builder()
                .passwordStatus(StringUtils.hasText(user.getPasswordHash()) ? "SET" : "NOT_SET")
                .build();
    }

    @Override
    @Transactional
    public void changePassword(UserPrincipal principal, ChangePasswordRequest request) {
        User user = getUserOrThrow(principal.getId());

        if (!StringUtils.hasText(user.getPasswordHash())) {
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);
            return;
        }

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
        return userDeviceRepository.findByUserId(principal.getId())
                .stream()
                .map(UserDeviceResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public UserDeviceResponse registerDevice(UserPrincipal principal, DeviceInfoRequest request) {
        User user = getUserOrThrow(principal.getId());

        if (request == null || !StringUtils.hasText(request.getDeviceId())) {
            throw new BadRequestException("deviceId là bắt buộc");
        }

        String deviceType = request.getDeviceType();
        if (!StringUtils.hasText(deviceType)) {
            throw new BadRequestException("Loại thiết bị là bắt buộc khi gửi deviceId");
        }

        UserDevice userDevice = userDeviceRepository.findByDeviceId(request.getDeviceId().trim())
                .orElseGet(UserDevice::new);

        userDevice.setUser(user);
        userDevice.setDeviceId(request.getDeviceId().trim());
        userDevice.setDeviceType(deviceType.trim().toLowerCase());
        userDevice.setDeviceToken(emptyToNull(request.getDeviceToken()));
        userDevice.setNotificationEnabled(StringUtils.hasText(request.getDeviceToken()));
        userDevice.setLastActiveAt(LocalDateTime.now());
        userDevice.setLastLoginAt(LocalDateTime.now());

        return UserDeviceResponse.from(userDeviceRepository.save(userDevice));
    }

    @Override
    @Transactional
    public void deleteMyDevice(UserPrincipal principal, Long deviceId) {
        userDeviceRepository.findByIdAndUserId(deviceId, principal.getId())
                .ifPresent(userDeviceRepository::delete);
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
