package com.petcare.backend.controller;

import com.petcare.backend.dto.ApiResponse;
import com.petcare.backend.dto.ChangePasswordRequest;
import com.petcare.backend.dto.UpdateProfileRequest;
import com.petcare.backend.dto.UpdateUserPreferencesRequest;
import com.petcare.backend.dto.UserDeviceResponse;
import com.petcare.backend.dto.UserResponse;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật hồ sơ thành công",
                userService.updateProfile(principal, request)
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

    @DeleteMapping("/me/devices/{deviceId}")
    public ResponseEntity<ApiResponse<Void>> deleteMyDevice(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long deviceId) {
        userService.deleteMyDevice(principal, deviceId);
        return ResponseEntity.ok(ApiResponse.success("Xóa thiết bị thành công", null));
    }
}
