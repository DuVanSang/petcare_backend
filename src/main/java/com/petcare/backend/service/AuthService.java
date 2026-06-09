package com.petcare.backend.service;

import com.petcare.backend.dto.AuthResponse;
import com.petcare.backend.dto.LoginRequest;
import com.petcare.backend.dto.RegisterRequest;
import com.petcare.backend.dto.RegisterResponse;
import com.petcare.backend.dto.ResendVerificationRequest;
import com.petcare.backend.dto.UserResponse;
import com.petcare.backend.dto.VerifyEmailRequest;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.model.User;
import com.petcare.backend.repository.UserRepository;
import com.petcare.backend.security.JwtService;
import com.petcare.backend.security.UserPrincipal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailVerificationService emailVerificationService;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email da duoc su dung");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName().trim());
        user.setPhoneNumber(request.getPhoneNumber());

        User savedUser = userRepository.save(user);
        emailVerificationService.createAndSendOtp(savedUser);

        return new RegisterResponse(savedUser.getId(), savedUser.getEmail(), savedUser.getEmailVerified());
    }

    @Transactional
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new BadRequestException("Email khong ton tai"));

        emailVerificationService.verify(user, request.getOtpCode());
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        String accessToken = jwtService.generateToken(UserPrincipal.from(savedUser));

        return new AuthResponse(accessToken, "Bearer", UserResponse.from(savedUser));
    }

    @Transactional
    public void resendVerificationCode(ResendVerificationRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new BadRequestException("Email khong ton tai"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BadRequestException("Email da duoc xac thuc");
        }

        emailVerificationService.createAndSendOtp(user);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        normalizeEmail(request.getEmail()),
                        request.getPassword()
                )
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new BadRequestException("Email hoac mat khau khong chinh xac"));

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BadRequestException("Vui long xac thuc email truoc khi dang nhap");
        }

        String accessToken = jwtService.generateToken(principal);
        return new AuthResponse(accessToken, "Bearer", UserResponse.from(user));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
