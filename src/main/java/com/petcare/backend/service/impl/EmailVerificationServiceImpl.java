package com.petcare.backend.service.impl;

import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.model.EmailVerificationToken;
import com.petcare.backend.model.User;
import com.petcare.backend.repository.EmailVerificationTokenRepository;
import com.petcare.backend.repository.UserRepository;
import com.petcare.backend.service.EmailVerificationService;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;

    @Value("${app.email-verification.otp-expiration-minutes:10}")
    private long otpExpirationMinutes;

    @Override
    @Transactional
    public String createOtp(User user) {
        invalidateActiveTokens(user.getId());
        return saveNewOtp(user);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String createOtpForExistingUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Người dùng không tồn tại"));
        invalidateActiveTokens(userId);
        return saveNewOtp(user);
    }

    private String saveNewOtp(User user) {
        String otpCode = String.format("%06d", RANDOM.nextInt(1_000_000));
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setOtpCode(otpCode);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes));
        tokenRepository.save(token);

        return otpCode;
    }

    @Override
    @Transactional
    public void verify(User user, String otpCode) {
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BadRequestException("Email đã được xác thực");
        }

        String normalizedOtpCode = otpCode == null ? "" : otpCode.trim();

        EmailVerificationToken token = tokenRepository
                .findTopByUserIdAndOtpCodeAndUsedAtIsNullOrderByCreatedAtDesc(user.getId(), normalizedOtpCode)
                .orElseThrow(() -> new BadRequestException("Mã OTP không hợp lệ"));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Mã OTP đã hết hạn");
        }

        token.setUsedAt(LocalDateTime.now());
        tokenRepository.save(token);
    }

    private void invalidateActiveTokens(Long userId) {
        tokenRepository.findByUserIdAndUsedAtIsNull(userId).forEach(token -> {
            token.setUsedAt(LocalDateTime.now());
            tokenRepository.save(token);
        });
    }
}
