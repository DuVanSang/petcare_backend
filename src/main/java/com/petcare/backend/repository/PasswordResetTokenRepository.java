package com.petcare.backend.repository;

import com.petcare.backend.model.PasswordResetToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findTopByUserIdAndOtpCodeAndUsedAtIsNullOrderByCreatedAtDesc(
            Long userId,
            String otpCode
    );

    List<PasswordResetToken> findByUserIdAndUsedAtIsNull(Long userId);
}
