package com.petcare.backend.service;

import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.model.EmailVerificationToken;
import com.petcare.backend.model.User;
import com.petcare.backend.repository.EmailVerificationTokenRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailService emailService;

    @Value("${app.email-verification.otp-expiration-minutes:10}")
    private long otpExpirationMinutes;

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

    @Transactional
    public void verify(User user, String otpCode) {
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BadRequestException("Email da duoc xac thuc");
        }

        EmailVerificationToken token = tokenRepository
                .findTopByUserIdAndOtpCodeAndUsedAtIsNullOrderByCreatedAtDesc(user.getId(), otpCode)
                .orElseThrow(() -> new BadRequestException("Ma OTP khong hop le"));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Ma OTP da het han");
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
