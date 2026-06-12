package com.petcare.backend.service.impl;

import com.petcare.backend.dto.UserResponse;
import com.petcare.backend.exception.ResourceNotFoundException;
import com.petcare.backend.repository.UserRepository;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    public UserResponse getCurrentUser(UserPrincipal principal) {
        return userRepository.findById(principal.getId())
                .map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
    }
}
