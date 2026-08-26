package com.petcare.backend.service;

import com.petcare.backend.dto.admin.user.request.AdminCreateUserRequest;
import com.petcare.backend.dto.admin.user.request.AdminUpdateUserRoleRequest;
import com.petcare.backend.dto.admin.user.request.AdminUpdateUserStatusRequest;
import com.petcare.backend.dto.admin.user.request.AdminUpdateUserRequest;
import com.petcare.backend.dto.admin.user.response.AdminUserDetailResponse;
import com.petcare.backend.dto.admin.user.response.AdminUserResponse;
import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.security.UserPrincipal;

public interface AdminUserService {
    PageResponse<AdminUserResponse> getUsers(
            String keyword,
            String role,
            String status,
            Boolean emailVerified,
            Boolean includeDeleted,
            int page,
            int size
    );

    AdminUserDetailResponse getUserDetail(Long userId);

    AdminUserDetailResponse createUser(AdminCreateUserRequest request);

    AdminUserDetailResponse updateUser(
            UserPrincipal currentAdmin,
            Long userId,
            AdminUpdateUserRequest request
    );

    AdminUserDetailResponse updateUserStatus(
            UserPrincipal currentAdmin,
            Long userId,
            AdminUpdateUserStatusRequest request
    );

    AdminUserDetailResponse updateUserRole(
            UserPrincipal currentAdmin,
            Long userId,
            AdminUpdateUserRoleRequest request
    );
}
