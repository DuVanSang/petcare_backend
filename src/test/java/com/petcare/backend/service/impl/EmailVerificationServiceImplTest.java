package com.petcare.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.model.EmailVerificationToken;
import com.petcare.backend.model.User;
import com.petcare.backend.repository.EmailVerificationTokenRepository;
import com.petcare.backend.service.EmailService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceImplTest {
    @Mock EmailVerificationTokenRepository tokens;
    @Mock EmailService emailService;
    private EmailVerificationServiceImpl service;

    @BeforeEach void setUp() {
        service = new EmailVerificationServiceImpl(tokens, emailService);
        ReflectionTestUtils.setField(service, "otpExpirationMinutes", 10L);
    }

    private User user(boolean verified) {
        User user = new User(); user.setId(1L); user.setEmail("pet@example.com"); user.setEmailVerified(verified); return user;
    }

    @Test void createAndSendOtp_invalidatesAllActiveTokensPersistsSixDigitTokenAndSendsEmail() {
        User user = user(false);
        EmailVerificationToken first = new EmailVerificationToken();
        EmailVerificationToken second = new EmailVerificationToken();
        when(tokens.findByUserIdAndUsedAtIsNull(1L)).thenReturn(List.of(first, second));
        LocalDateTime before = LocalDateTime.now();

        service.createAndSendOtp(user);

        assertThat(first.getUsedAt()).isNotNull(); assertThat(second.getUsedAt()).isNotNull();
        ArgumentCaptor<EmailVerificationToken> saved = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokens, times(3)).save(saved.capture());
        EmailVerificationToken fresh = saved.getAllValues().get(2);
        assertThat(fresh.getUser()).isSameAs(user);
        assertThat(fresh.getOtpCode()).matches("\\d{6}");
        assertThat(fresh.getExpiresAt()).isAfterOrEqualTo(before.plusMinutes(10).minusSeconds(1));
        verify(emailService).sendVerificationOtp("pet@example.com", fresh.getOtpCode());
    }

    @Test void createAndSendOtp_withNoActiveTokenOnlySavesAndSendsNewOtp() {
        User user = user(false);
        when(tokens.findByUserIdAndUsedAtIsNull(1L)).thenReturn(List.of());
        service.createAndSendOtp(user);
        verify(tokens).save(any(EmailVerificationToken.class));
        verify(emailService).sendVerificationOtp(eq("pet@example.com"), matches("\\d{6}"));
    }

    @Test void verify_rejectsAlreadyVerifiedAndMissingTokenWithoutSaving() {
        assertThatThrownBy(() -> service.verify(user(true), "123456")).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(tokens);

        User user = user(false);
        when(tokens.findTopByUserIdAndOtpCodeAndUsedAtIsNullOrderByCreatedAtDesc(1L, "123456")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.verify(user, "123456")).isInstanceOf(BadRequestException.class);
        verify(tokens, never()).save(any());
    }

    @Test void verify_rejectsExpiredTokenAndMarksValidTokenUsed() {
        User user = user(false);
        EmailVerificationToken expired = new EmailVerificationToken(); expired.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        when(tokens.findTopByUserIdAndOtpCodeAndUsedAtIsNullOrderByCreatedAtDesc(1L, "000001")).thenReturn(Optional.of(expired));
        assertThatThrownBy(() -> service.verify(user, "000001")).isInstanceOf(BadRequestException.class);
        verify(tokens, never()).save(expired);

        EmailVerificationToken valid = new EmailVerificationToken(); valid.setExpiresAt(LocalDateTime.now().plusMinutes(1));
        when(tokens.findTopByUserIdAndOtpCodeAndUsedAtIsNullOrderByCreatedAtDesc(1L, "000002")).thenReturn(Optional.of(valid));
        service.verify(user, "000002");
        assertThat(valid.getUsedAt()).isNotNull(); verify(tokens).save(valid);
    }
}
