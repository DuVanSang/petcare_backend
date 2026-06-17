package com.petcare.backend.service.impl;

import com.petcare.backend.dto.auth.response.AuthResponse;
import com.petcare.backend.dto.auth.request.ForgotPasswordRequest;
import com.petcare.backend.dto.auth.request.DeviceInfoRequest;
import com.petcare.backend.dto.auth.request.LoginRequest;
import com.petcare.backend.dto.auth.request.LogoutRequest;
import com.petcare.backend.dto.auth.request.RefreshTokenRequest;
import com.petcare.backend.dto.auth.request.RegisterRequest;
import com.petcare.backend.dto.auth.response.RegisterResponse;
import com.petcare.backend.dto.auth.request.ResendVerificationRequest;
import com.petcare.backend.dto.auth.request.ResetPasswordRequest;
import com.petcare.backend.dto.user.response.UserResponse;
import com.petcare.backend.dto.auth.request.VerifyEmailRequest;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.model.PasswordResetToken;
import com.petcare.backend.model.RefreshToken;
import com.petcare.backend.model.User;
import com.petcare.backend.model.UserDevice;
import com.petcare.backend.repository.PasswordResetTokenRepository;
import com.petcare.backend.repository.RefreshTokenRepository;
import com.petcare.backend.repository.UserDeviceRepository;
import com.petcare.backend.repository.UserRepository;
import com.petcare.backend.security.JwtService;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.AuthService;
import com.petcare.backend.service.EmailService;
import com.petcare.backend.service.EmailVerificationService;
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
public class AuthServiceImpl implements AuthService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailVerificationService emailVerificationService;
    private final EmailService emailService;

    @Value("${app.refresh-token.expiration-ms}")
    private long refreshTokenExpirationMs;

    @Value("${app.password-reset.otp-expiration-minutes:10}")
    private long passwordResetOtpExpirationMinutes;

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email đã được sử dụng");
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

    @Override
    @Transactional
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new BadRequestException("Email không tồn tại"));

        emailVerificationService.verify(user, request.getOtpCode());
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        String accessToken = jwtService.generateToken(UserPrincipal.from(savedUser));
        String refreshToken = createRefreshToken(savedUser).getToken();

        return new AuthResponse(accessToken, refreshToken, "Bearer", UserResponse.from(savedUser));
    }

    @Override
    @Transactional
    public void resendVerificationCode(ResendVerificationRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new BadRequestException("Email không tồn tại"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BadRequestException("Email đã được xác thực");
        }

        emailVerificationService.createAndSendOtp(user);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        normalizeEmail(request.getEmail()),
                        request.getPassword()
                )
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new BadRequestException("Email hoặc mật khẩu không chính xác"));

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BadRequestException("Vui lòng xác thực email trước khi đăng nhập");
        }

        upsertUserDevice(user, request.getDevice());

        String accessToken = jwtService.generateToken(principal);
        String refreshToken = createRefreshToken(user).getToken();
        return new AuthResponse(accessToken, refreshToken, "Bearer", UserResponse.from(user));
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = validateRefreshToken(request.getRefreshToken());
        User user = refreshToken.getUser();

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BadRequestException("Vui lòng xác thực email trước khi đăng nhập");
        }

        revokeRefreshToken(refreshToken);
        String newAccessToken = jwtService.generateToken(UserPrincipal.from(user));
        String newRefreshToken = createRefreshToken(user).getToken();

        return new AuthResponse(newAccessToken, newRefreshToken, "Bearer", UserResponse.from(user));
    }

    @Override
    @Transactional
    public void logout(LogoutRequest request) {
        refreshTokenRepository.findByToken(request.getRefreshToken())
                .filter(token -> !token.isRevoked())
                .ifPresent(this::revokeRefreshToken);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(normalizeEmail(request.getEmail()))
                .filter(user -> "active".equalsIgnoreCase(user.getStatus()))
                .ifPresent(user -> {
                    invalidateActivePasswordResetTokens(user.getId());

                    PasswordResetToken token = new PasswordResetToken();
                    token.setUser(user);
                    token.setOtpCode(generateOtpCode());
                    token.setExpiresAt(LocalDateTime.now().plusMinutes(passwordResetOtpExpirationMinutes));
                    passwordResetTokenRepository.save(token);

                    emailService.sendPasswordResetOtp(user.getEmail(), token.getOtpCode());
                });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new BadRequestException("Mã OTP không hợp lệ"));

        PasswordResetToken token = passwordResetTokenRepository
                .findTopByUserIdAndOtpCodeAndUsedAtIsNullOrderByCreatedAtDesc(user.getId(), request.getOtpCode())
                .orElseThrow(() -> new BadRequestException("Mã OTP không hợp lệ"));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Mã OTP đã hết hạn");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        token.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(token);

        revokeAllActiveRefreshTokens(user.getId());
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
                .orElseThrow(() -> new BadRequestException("Refresh token không hợp lệ"));

        if (refreshToken.isRevoked()) {
            throw new BadRequestException("Refresh token đã bị thu hồi");
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Refresh token đã hết hạn");
        }

        return refreshToken;
    }

    private void revokeRefreshToken(RefreshToken refreshToken) {
        refreshToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);
    }

    private void revokeAllActiveRefreshTokens(Long userId) {
        refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId).forEach(this::revokeRefreshToken);
    }

    private void upsertUserDevice(User user, DeviceInfoRequest device) {
        if (device == null || !StringUtils.hasText(device.getDeviceId())) {
            return;
        }

        UserDevice userDevice = userDeviceRepository.findByDeviceId(device.getDeviceId().trim())
                .orElseGet(UserDevice::new);

        userDevice.setUser(user);
        userDevice.setDeviceId(device.getDeviceId().trim());
        userDevice.setDeviceName(trimToNull(device.getDeviceName()));
        userDevice.setDeviceType(StringUtils.hasText(device.getDeviceType()) ? device.getDeviceType().trim() : "unknown");
        userDevice.setDeviceToken(trimToNull(device.getDeviceToken()));
        userDevice.setNotificationEnabled(device.getNotificationEnabled() != null ? device.getNotificationEnabled() : true);
        userDevice.setAppVersion(trimToNull(device.getAppVersion()));
        userDevice.setOsVersion(trimToNull(device.getOsVersion()));
        userDevice.setLastActiveAt(LocalDateTime.now());
        userDevice.setLastLoginAt(LocalDateTime.now());

        userDeviceRepository.save(userDevice);
    }

    private String generateRefreshTokenValue() {
        byte[] bytes = new byte[64];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateOtpCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private void invalidateActivePasswordResetTokens(Long userId) {
        passwordResetTokenRepository.findByUserIdAndUsedAtIsNull(userId).forEach(token -> {
            token.setUsedAt(LocalDateTime.now());
            passwordResetTokenRepository.save(token);
        });
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
