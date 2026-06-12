package com.petcare.backend.service;

import com.petcare.backend.dto.UserResponse;
import com.petcare.backend.security.UserPrincipal;

public interface UserService {
    UserResponse getCurrentUser(UserPrincipal principal);
}
