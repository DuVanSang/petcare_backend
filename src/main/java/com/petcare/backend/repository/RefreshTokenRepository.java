package com.petcare.backend.repository;

import com.petcare.backend.model.RefreshToken;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findByUserIdAndIsRevokedFalse(Long userId);

    List<RefreshToken> findByUserIdAndIsRevokedFalseAndExpiresAtAfter(Long userId, LocalDateTime now);

    Optional<RefreshToken> findByIdAndUserId(Long id, Long userId);
}
