package com.petcare.backend.repository;

import com.petcare.backend.model.EmailVerificationToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findTopByUserIdAndOtpCodeAndUsedAtIsNullOrderByCreatedAtDesc(
            Long userId,
            String otpCode
    );

    List<EmailVerificationToken> findByUserIdAndUsedAtIsNull(Long userId);
}
