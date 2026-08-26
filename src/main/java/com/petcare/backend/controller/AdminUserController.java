package com.petcare.backend.controller;

import com.petcare.backend.dto.admin.user.request.AdminCreateUserRequest;
import com.petcare.backend.dto.admin.user.request.AdminUpdateUserRequest;
import com.petcare.backend.dto.admin.user.request.AdminUpdateUserRoleRequest;
import com.petcare.backend.dto.admin.user.request.AdminUpdateUserStatusRequest;
import com.petcare.backend.dto.admin.user.response.AdminUserDetailResponse;
import com.petcare.backend.dto.admin.user.response.AdminUserResponse;
import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.dto.upload.UploadFileResponse;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.AdminUserService;
import com.petcare.backend.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Users", description = "Quản lý người dùng")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {
    private final AdminUserService adminUserService;
    private final FileStorageService fileStorageService;

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Tải ảnh đại diện người dùng")
    public ResponseEntity<ApiResponse<UploadFileResponse>> uploadUserAvatar(
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Tải ảnh đại diện thành công",
                fileStorageService.storeUserProfileImage(file, null, "avatar")
        ));
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách người dùng")
    public ResponseEntity<ApiResponse<PageResponse<AdminUserResponse>>> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean emailVerified,
            @RequestParam(defaultValue = "false") Boolean includeDeleted,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách người dùng thành công",
                adminUserService.getUsers(keyword, role, status, emailVerified, includeDeleted, page, size)
        ));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Xem chi tiết người dùng")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> getUserDetail(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy chi tiết người dùng thành công",
                adminUserService.getUserDetail(userId)
        ));
    }

    @PostMapping
    @Operation(summary = "Tạo người dùng mới")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> createUser(
            @Valid @RequestBody AdminCreateUserRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Tạo người dùng thành công",
                adminUserService.createUser(request)
        ));
    }

    @PatchMapping("/{userId}")
    @Operation(summary = "Cập nhật thông tin người dùng")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> updateUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long userId,
            @Valid @RequestBody AdminUpdateUserRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật người dùng thành công",
                adminUserService.updateUser(principal, userId, request)
        ));
    }

    @PatchMapping("/{userId}/status")
    @Operation(summary = "Cập nhật trạng thái người dùng")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> updateUserStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long userId,
            @Valid @RequestBody AdminUpdateUserStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật trạng thái người dùng thành công",
                adminUserService.updateUserStatus(principal, userId, request)
        ));
    }

    @PatchMapping("/{userId}/role")
    @Operation(summary = "Cập nhật vai trò người dùng")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> updateUserRole(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long userId,
            @Valid @RequestBody AdminUpdateUserRoleRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật vai trò người dùng thành công",
                adminUserService.updateUserRole(principal, userId, request)
        ));
    }
}
