package com.petcare.backend.service.impl;

import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.model.EmailVerificationToken;
import com.petcare.backend.model.User;
import com.petcare.backend.repository.EmailVerificationTokenRepository;
import com.petcare.backend.service.EmailService;
import com.petcare.backend.service.EmailVerificationService;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailService emailService;

    @Value("${app.email-verification.otp-expiration-minutes:10}")
    private long otpExpirationMinutes;

    @Override
    @Transactional
    public void createAndSendOtp(User user) {
        invalidateActiveTokens(user.getId());

        String otpCode = String.format("%06d", RANDOM.nextInt(1_000_000));
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setOtpCode(otpCode);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes));
        tokenRepository.save(token);

        emailService.sendVerificationOtp(user.getEmail(), otpCode);
    }

    @Override
    @Transactional
    public void verify(User user, String otpCode) {
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BadRequestException("Email đã được xác thực");
        }

        EmailVerificationToken token = tokenRepository
                .findTopByUserIdAndOtpCodeAndUsedAtIsNullOrderByCreatedAtDesc(user.getId(), otpCode)
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
