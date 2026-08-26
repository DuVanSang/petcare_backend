package com.petcare.backend.controller;

import com.petcare.backend.dto.auth.request.DeviceInfoRequest;
import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.dto.user.request.ChangePasswordRequest;
import com.petcare.backend.dto.user.request.UpdateProfileRequest;
import com.petcare.backend.dto.user.request.UpdateUserPreferencesRequest;
import com.petcare.backend.dto.user.response.PasswordStatusResponse;
import com.petcare.backend.dto.user.response.UserDeviceResponse;
import com.petcare.backend.dto.user.response.UserResponse;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy thông tin người dùng thành công",
                userService.getCurrentUser(principal)
        ));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy thông tin người dùng thành công",
                userService.getUserById(userId)
        ));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật hồ sơ thành công",
                userService.updateProfile(principal, request)
        ));
    }

    @PatchMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserResponse>> uploadAvatar(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật ảnh đại diện thành công",
                userService.uploadAvatar(principal, requireFile("Vui lòng chọn ảnh đại diện", file, avatar, image))
        ));
    }

    @PatchMapping(value = "/me/cover-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserResponse>> uploadCoverImage(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestPart(value = "cover", required = false) MultipartFile cover,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật ảnh bìa thành công",
                userService.uploadCoverImage(principal, requireFile("Vui lòng chọn ảnh bìa", file, cover, image))
        ));
    }

    @PatchMapping("/me/preferences")
    public ResponseEntity<ApiResponse<UserResponse>> updatePreferences(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateUserPreferencesRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật cấu hình cá nhân thành công",
                userService.updatePreferences(principal, request)
        ));
    }

    @GetMapping("/me/password-status")
    public ResponseEntity<ApiResponse<PasswordStatusResponse>> getPasswordStatus(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy trạng thái mật khẩu thành công",
                userService.getPasswordStatus(principal)
        ));
    }

    @PostMapping("/me/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal, request);
        return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công", null));
    }

    @GetMapping("/me/devices")
    public ResponseEntity<ApiResponse<List<UserDeviceResponse>>> getMyDevices(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách thiết bị thành công",
                userService.getMyDevices(principal)
        ));
    }

    @PostMapping("/me/devices")
    public ResponseEntity<ApiResponse<UserDeviceResponse>> registerDevice(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DeviceInfoRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đăng ký thiết bị thành công",
                userService.registerDevice(principal, request)
        ));
    }

    @DeleteMapping("/me/devices/{deviceId}")
    public ResponseEntity<ApiResponse<Void>> deleteMyDevice(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long deviceId) {
        userService.deleteMyDevice(principal, deviceId);
        return ResponseEntity.ok(ApiResponse.success("Xóa thiết bị thành công", null));
    }

    private MultipartFile requireFile(String message, MultipartFile... files) {
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                return file;
            }
        }
        throw new BadRequestException(message);
    }
}
