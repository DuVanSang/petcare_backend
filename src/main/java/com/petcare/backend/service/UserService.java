package com.petcare.backend.service;

import com.petcare.backend.dto.user.request.ChangePasswordRequest;
import com.petcare.backend.dto.user.request.UpdateProfileRequest;
import com.petcare.backend.dto.user.request.UpdateUserPreferencesRequest;
import com.petcare.backend.dto.user.response.UserDeviceResponse;
import com.petcare.backend.dto.user.response.UserResponse;
import com.petcare.backend.security.UserPrincipal;
import java.util.List;

public interface UserService {
    UserResponse getCurrentUser(UserPrincipal principal);

    UserResponse updateProfile(UserPrincipal principal, UpdateProfileRequest request);

    UserResponse updatePreferences(UserPrincipal principal, UpdateUserPreferencesRequest request);

    void changePassword(UserPrincipal principal, ChangePasswordRequest request);

    List<UserDeviceResponse> getMyDevices(UserPrincipal principal);

    void deleteMyDevice(UserPrincipal principal, Long deviceId);
}
