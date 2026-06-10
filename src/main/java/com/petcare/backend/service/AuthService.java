package com.petcare.backend.service;

import com.petcare.backend.dto.AuthResponse;
import com.petcare.backend.dto.LoginRequest;
import com.petcare.backend.dto.RefreshTokenRequest;
import com.petcare.backend.dto.RegisterRequest;
import com.petcare.backend.dto.RegisterResponse;
import com.petcare.backend.dto.ResendVerificationRequest;
import com.petcare.backend.dto.UserResponse;
import com.petcare.backend.dto.VerifyEmailRequest;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.model.RefreshToken;
import com.petcare.backend.model.User;
import com.petcare.backend.model.UserDevice;
import com.petcare.backend.repository.RefreshTokenRepository;
import com.petcare.backend.repository.UserDeviceRepository;
import com.petcare.backend.repository.UserRepository;
import com.petcare.backend.security.JwtService;
import com.petcare.backend.security.UserPrincipal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailVerificationService emailVerificationService;

    @Value("${app.refresh-token.expiration-ms}")
    private long refreshTokenExpirationMs;

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
        saveUserDeviceIfPresent(request, savedUser);
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
        String refreshToken = createRefreshToken(savedUser).getToken();

        return new AuthResponse(accessToken, refreshToken, "Bearer", UserResponse.from(savedUser));
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
        String refreshToken = createRefreshToken(user).getToken();
        return new AuthResponse(accessToken, refreshToken, "Bearer", UserResponse.from(user));
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = validateRefreshToken(request.getRefreshToken());
        User user = refreshToken.getUser();

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BadRequestException("Vui long xac thuc email truoc khi dang nhap");
        }

        String newAccessToken = jwtService.generateToken(UserPrincipal.from(user));
        revokeRefreshToken(refreshToken);
        String newRefreshToken = createRefreshToken(user).getToken();

        return new AuthResponse(newAccessToken, newRefreshToken, "Bearer", UserResponse.from(user));
    }

    private RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(generateRefreshTokenValue());
        refreshToken.setExpiresAt(LocalDateTime.now().plus(Duration.ofMillis(refreshTokenExpirationMs)));
        return refreshTokenRepository.save(refreshToken);
    }

    private RefreshToken validateRefreshToken(String tokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new BadRequestException("Refresh token khong hop le"));

        if (refreshToken.getRevokedAt() != null) {
            throw new BadRequestException("Refresh token da bi thu hoi");
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Refresh token da het han");
        }

        return refreshToken;
    }

    private void revokeRefreshToken(RefreshToken refreshToken) {
        refreshToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);
    }

    private String generateRefreshTokenValue() {
        byte[] bytes = new byte[64];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void saveUserDeviceIfPresent(RegisterRequest request, User user) {
        if (!StringUtils.hasText(request.getDeviceToken())) {
            return;
        }

        if (!StringUtils.hasText(request.getDeviceType())) {
            throw new BadRequestException("Device type la bat buoc khi gui device token");
        }

        String deviceType = request.getDeviceType().trim().toLowerCase();

        UserDevice userDevice = userDeviceRepository.findByDeviceToken(request.getDeviceToken().trim())
                .orElseGet(UserDevice::new);
        userDevice.setUser(user);
        userDevice.setDeviceToken(request.getDeviceToken().trim());
        userDevice.setDeviceType(deviceType);
        userDeviceRepository.save(userDevice);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
