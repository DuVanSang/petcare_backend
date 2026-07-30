package com.petcare.backend.service.impl;

import com.petcare.backend.dto.auth.request.DeviceInfoRequest;
import com.petcare.backend.dto.auth.request.FirebaseLoginRequest;
import com.petcare.backend.dto.auth.request.ForgotPasswordRequest;
import com.petcare.backend.dto.auth.request.GoogleLoginRequest;
import com.petcare.backend.dto.auth.request.LoginRequest;
import com.petcare.backend.dto.auth.request.LogoutRequest;
import com.petcare.backend.dto.auth.request.RefreshTokenRequest;
import com.petcare.backend.dto.auth.request.RegisterRequest;
import com.petcare.backend.dto.auth.request.ResendVerificationRequest;
import com.petcare.backend.dto.auth.request.ResetPasswordRequest;
import com.petcare.backend.dto.auth.request.VerifyEmailRequest;
import com.petcare.backend.dto.auth.response.AuthResponse;
import com.petcare.backend.dto.auth.response.OtpDeliveryResponse;
import com.petcare.backend.dto.auth.response.RegisterResponse;
import com.petcare.backend.dto.user.response.UserResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.exception.EmailNotVerifiedException;
import com.petcare.backend.model.PasswordResetToken;
import com.petcare.backend.model.RefreshToken;
import com.petcare.backend.model.User;
import com.petcare.backend.model.UserDevice;
import com.petcare.backend.model.UserSocialAccount;
import com.petcare.backend.model.enums.SocialProvider;
import com.petcare.backend.repository.PasswordResetTokenRepository;
import com.petcare.backend.repository.RefreshTokenRepository;
import com.petcare.backend.repository.UserDeviceRepository;
import com.petcare.backend.repository.UserRepository;
import com.petcare.backend.repository.UserSocialAccountRepository;
import com.petcare.backend.security.JwtService;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.AuthService;
import com.petcare.backend.service.EmailService;
import com.petcare.backend.service.EmailVerificationService;
import com.petcare.backend.service.GoogleTokenService;
import com.petcare.backend.service.GoogleUserPayload;
import org.springframework.beans.factory.ObjectProvider;
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
    private static final String INVALID_RESET_OTP_MESSAGE = "Mã OTP không hợp lệ hoặc đã hết hạn";

    private final UserRepository userRepository;
    private final UserSocialAccountRepository userSocialAccountRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailVerificationService emailVerificationService;
    private final EmailService emailService;
    private final GoogleTokenService googleTokenService;
    private final ObjectProvider<FirebaseAuth> firebaseAuthProvider;

    @Value("${app.refresh-token.expiration-ms}")
    private long refreshTokenExpirationMs;

    @Value("${app.password-reset.otp-expiration-minutes:10}")
    private long passwordResetOtpExpirationMinutes;

    @Value("${app.mail.dev-expose-otp:false}")
    private boolean devExposeOtp;

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        String phoneNumber = trimToNull(request.getPhoneNumber());
        User existingUser = userRepository.findByEmail(email).orElse(null);
        if (existingUser != null && !Boolean.TRUE.equals(existingUser.getEmailVerified())) {
            if (phoneNumber != null && userRepository.existsByPhoneNumberAndIdNot(phoneNumber, existingUser.getId())) {
                throw new BadRequestException("Số điện thoại đã được sử dụng");
            }

            existingUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            existingUser.setFullName(request.getFullName().trim());
            existingUser.setPhoneNumber(phoneNumber);

            User savedUser = userRepository.save(existingUser);
            String otpCode = emailVerificationService.createOtp(savedUser);
            boolean emailSent = emailService.sendVerificationOtp(savedUser.getEmail(), otpCode);

            return new RegisterResponse(
                    savedUser.getId(),
                    savedUser.getEmail(),
                    savedUser.getEmailVerified(),
                    emailSent,
                    resolveDevOtp(emailSent, otpCode),
                    true,
                    true
            );
        }
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email đã được sử dụng");
        }

        if (phoneNumber != null && userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new BadRequestException("Số điện thoại đã được sử dụng");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName().trim());
        user.setPhoneNumber(phoneNumber);

        User savedUser = userRepository.save(user);
        String otpCode = emailVerificationService.createOtp(savedUser);
        boolean emailSent = emailService.sendVerificationOtp(savedUser.getEmail(), otpCode);

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getEmailVerified(),
                emailSent,
                resolveDevOtp(emailSent, otpCode)
        );
    }

    @Override
    @Transactional(timeout = 10)
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new BadRequestException("Email không tồn tại"));

        emailVerificationService.verify(user, request.getOtpCode());
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());

        User savedUser = userRepository.saveAndFlush(user);
        String accessToken = jwtService.generateToken(UserPrincipal.from(savedUser));
        String refreshToken = createRefreshToken(savedUser).getToken();
        return new AuthResponse(accessToken, refreshToken, "Bearer", UserResponse.from(savedUser));
    }

    @Override
    @Transactional
    public OtpDeliveryResponse resendVerificationCode(ResendVerificationRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new BadRequestException("Email không tồn tại"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BadRequestException("Email đã được xác thực");
        }

        String otpCode = emailVerificationService.createOtp(user);
        boolean emailSent = emailService.sendVerificationOtp(user.getEmail(), otpCode);
        return buildOtpDelivery(emailSent, otpCode);
    }

    @Override
    @Transactional(noRollbackFor = EmailNotVerifiedException.class)
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

        ensureActiveAccount(user);

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            String otpCode = emailVerificationService.createOtpForExistingUser(user.getId());
            emailService.sendVerificationOtp(user.getEmail(), otpCode);
            throw new EmailNotVerifiedException(user.getEmail());
        }

        String accessToken = jwtService.generateToken(principal);
        String refreshToken = createRefreshToken(user).getToken();
        return new AuthResponse(accessToken, refreshToken, "Bearer", UserResponse.from(user));
    }

    @Override
    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleUserPayload payload = googleTokenService.verify(request.getIdToken());
        String googleUserId = payload.getSubject();
        String email = payload.getEmail() == null ? null : payload.getEmail().trim().toLowerCase();
        if (!StringUtils.hasText(email)) {
            throw new BadRequestException("Không lấy được email từ tài khoản Google");
        }

        User user = userSocialAccountRepository
                .findByProviderAndProviderUserId(SocialProvider.GOOGLE, googleUserId)
                .map(UserSocialAccount::getUser)
                .orElseGet(() -> findOrCreateUserByGoogle(email, googleUserId, payload));

        if ("banned".equalsIgnoreCase(user.getStatus())) {
            throw new BadRequestException("Tài khoản đã bị khóa");
        }

        String accessToken = jwtService.generateToken(UserPrincipal.from(user));
        String refreshToken = createRefreshToken(user).getToken();
        return new AuthResponse(accessToken, refreshToken, "Bearer", UserResponse.from(user));
    }

    @Override
    @Transactional
    public AuthResponse loginWithFirebase(FirebaseLoginRequest request) {
        FirebaseAuth firebaseAuth = firebaseAuthProvider.getIfAvailable();
        if (firebaseAuth == null) {
            throw new BadRequestException("Firebase Auth chưa được cấu hình trên backend");
        }

        FirebaseToken token;
        try {
            token = firebaseAuth.verifyIdToken(request.getIdToken());
        } catch (FirebaseAuthException ex) {
            throw new BadRequestException("Firebase ID token không hợp lệ hoặc đã hết hạn");
        }

        String firebaseUid = token.getUid();
        String email = token.getEmail() == null ? null : token.getEmail().trim().toLowerCase();
        if (!StringUtils.hasText(email)) {
            throw new BadRequestException("Không lấy được email từ Firebase Auth");
        }

        User user = userSocialAccountRepository
                .findByProviderAndProviderUserId(SocialProvider.FIREBASE, firebaseUid)
                .map(UserSocialAccount::getUser)
                .orElseGet(() -> findOrCreateUserByFirebase(email, firebaseUid, token));

        ensureActiveAccount(user);

        String accessToken = jwtService.generateToken(UserPrincipal.from(user));
        String refreshToken = createRefreshToken(user).getToken();
        return new AuthResponse(accessToken, refreshToken, "Bearer", UserResponse.from(user));
    }

    private User findOrCreateUserByGoogle(String email, String googleUserId, GoogleUserPayload payload) {
        User user = userRepository.findByEmail(email).orElseGet(() -> createGoogleUser(email, payload));

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            user.setEmailVerified(true);
            user.setEmailVerifiedAt(LocalDateTime.now());
            user = userRepository.save(user);
        }

        linkGoogleAccount(user, googleUserId);
        return user;
    }

    private User findOrCreateUserByFirebase(String email, String firebaseUid, FirebaseToken token) {
        User user = userRepository.findByEmail(email).orElseGet(() -> createFirebaseUser(email, token));

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            user.setEmailVerified(true);
            user.setEmailVerifiedAt(LocalDateTime.now());
            user = userRepository.save(user);
        }

        linkFirebaseAccount(user, firebaseUid);
        return user;
    }

    private User createFirebaseUser(String email, FirebaseToken token) {
        User user = new User();
        user.setEmail(email);
        user.setFullName(resolveFirebaseFullName(token, email));
        Object picture = token.getClaims().get("picture");
        if (picture instanceof String pictureUrl && StringUtils.hasText(pictureUrl)) {
            user.setAvatarUrl(pictureUrl);
        }
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    private void linkFirebaseAccount(User user, String firebaseUid) {
        boolean alreadyLinked = userSocialAccountRepository
                .findByProviderAndProviderUserId(SocialProvider.FIREBASE, firebaseUid)
                .isPresent();
        if (alreadyLinked) {
            return;
        }

        UserSocialAccount socialAccount = new UserSocialAccount();
        socialAccount.setUser(user);
        socialAccount.setProvider(SocialProvider.FIREBASE);
        socialAccount.setProviderUserId(firebaseUid);
        userSocialAccountRepository.save(socialAccount);
    }

    private User createGoogleUser(String email, GoogleUserPayload payload) {
        User user = new User();
        user.setEmail(email);
        user.setFullName(resolveGoogleFullName(payload, email));
        user.setAvatarUrl(payload.getPicture());
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    private void linkGoogleAccount(User user, String googleUserId) {
        boolean alreadyLinked = userSocialAccountRepository
                .findByProviderAndProviderUserId(SocialProvider.GOOGLE, googleUserId)
                .isPresent();
        if (alreadyLinked) {
            return;
        }

        UserSocialAccount socialAccount = new UserSocialAccount();
        socialAccount.setUser(user);
        socialAccount.setProvider(SocialProvider.GOOGLE);
        socialAccount.setProviderUserId(googleUserId);
        userSocialAccountRepository.save(socialAccount);
    }

    private String resolveGoogleFullName(GoogleUserPayload payload, String email) {
        String name = payload.getName();
        if (StringUtils.hasText(name)) {
            return name.trim();
        }
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : email;
    }

    private String resolveFirebaseFullName(FirebaseToken token, String email) {
        String name = token.getName();
        if (StringUtils.hasText(name)) {
            return name.trim();
        }
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : email;
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = validateRefreshToken(request.getRefreshToken());
        User user = refreshToken.getUser();

        ensureActiveAccount(user);

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
    public OtpDeliveryResponse forgotPassword(ForgotPasswordRequest request) {
        return userRepository.findByEmail(normalizeEmail(request.getEmail()))
                .filter(user -> "active".equalsIgnoreCase(user.getStatus()))
                .map(user -> {
                    invalidateActivePasswordResetTokens(user.getId());

                    PasswordResetToken token = new PasswordResetToken();
                    token.setUser(user);
                    token.setOtpCode(generateOtpCode());
                    token.setExpiresAt(LocalDateTime.now().plusMinutes(passwordResetOtpExpirationMinutes));
                    passwordResetTokenRepository.save(token);

                    boolean emailSent = emailService.sendPasswordResetOtp(user.getEmail(), token.getOtpCode());
                    return buildOtpDelivery(emailSent, token.getOtpCode());
                })
                .orElseGet(() -> new OtpDeliveryResponse(false, null));
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new BadRequestException(INVALID_RESET_OTP_MESSAGE));

        if (!"active".equalsIgnoreCase(user.getStatus())) {
            throw new BadRequestException(INVALID_RESET_OTP_MESSAGE);
        }

        PasswordResetToken token = passwordResetTokenRepository
                .findTopByUserIdAndOtpCodeAndUsedAtIsNullOrderByCreatedAtDesc(user.getId(), request.getOtpCode())
                .orElseThrow(() -> new BadRequestException(INVALID_RESET_OTP_MESSAGE));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException(INVALID_RESET_OTP_MESSAGE);
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
        return refreshTokenRepository.saveAndFlush(refreshToken);
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

        String deviceType = device.getDeviceType();
        if (!StringUtils.hasText(deviceType)) {
            throw new BadRequestException("Loại thiết bị là bắt buộc khi gửi deviceId");
        }

        UserDevice userDevice = userDeviceRepository.findByDeviceId(device.getDeviceId().trim())
                .orElseGet(UserDevice::new);

        userDevice.setUser(user);
        userDevice.setDeviceId(device.getDeviceId().trim());
        userDevice.setDeviceType(deviceType.trim().toLowerCase());
        userDevice.setDeviceToken(trimToNull(device.getDeviceToken()));
        userDevice.setNotificationEnabled(StringUtils.hasText(device.getDeviceToken()));
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

    private void ensureActiveAccount(User user) {
        if (!"active".equalsIgnoreCase(user.getStatus())) {
            throw new BadRequestException("Tài khoản đã bị khóa");
        }
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

    private OtpDeliveryResponse buildOtpDelivery(boolean emailSent, String otpCode) {
        return new OtpDeliveryResponse(emailSent, resolveDevOtp(emailSent, otpCode));
    }

    private String resolveDevOtp(boolean emailSent, String otpCode) {
        if (emailSent || !devExposeOtp) {
            return null;
        }
        return otpCode;
    }
}
