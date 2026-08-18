package com.petcare.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.petcare.backend.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {
    @Mock ObjectProvider<JavaMailSender> provider;
    @Mock JavaMailSender mailSender;
    private EmailServiceImpl service;

    @BeforeEach void setUp() {
        service = new EmailServiceImpl(provider);
        ReflectionTestUtils.setField(service, "senderEmail", "noreply@petcare.test");
    }

    @Test void logProviderDoesNotResolveOrCallMailSenderForVerificationAndResetOtp() {
        ReflectionTestUtils.setField(service, "mailProvider", "log");
        service.sendVerificationOtp(null, "");
        service.sendPasswordResetOtp("pet@example.com", "123456");
        verifyNoInteractions(provider, mailSender);
    }

    @Test void smtpProviderFallsBackWhenSenderOrSenderEmailIsMissing() {
        ReflectionTestUtils.setField(service, "mailProvider", "SMTP");
        when(provider.getIfAvailable()).thenReturn(null);
        assertThat(service.sendVerificationOtp("pet@example.com", "123456")).isFalse();

        when(provider.getIfAvailable()).thenReturn(mailSender);
        ReflectionTestUtils.setField(service, "senderEmail", "  ");
        assertThat(service.sendPasswordResetOtp("pet@example.com", "654321")).isFalse();
        verifyNoInteractions(mailSender);
    }

    @Test void smtpProviderSendsVerificationAndResetMessagesWithExpectedArguments() {
        ReflectionTestUtils.setField(service, "mailProvider", "smtp");
        when(provider.getIfAvailable()).thenReturn(mailSender);
        service.sendVerificationOtp("pet@example.com", "123456");
        service.sendPasswordResetOtp("pet@example.com", "654321");

        ArgumentCaptor<SimpleMailMessage> messages = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(2)).send(messages.capture());
        SimpleMailMessage verification = messages.getAllValues().get(0);
        SimpleMailMessage reset = messages.getAllValues().get(1);
        assertThat(verification.getFrom()).isEqualTo("noreply@petcare.test");
        assertThat(verification.getTo()).containsExactly("pet@example.com");
        assertThat(verification.getSubject()).contains("xác thực");
        assertThat(verification.getText()).contains("123456");
        assertThat(reset.getSubject()).contains("đặt lại");
        assertThat(reset.getText()).contains("654321");
    }

    @Test void smtpProviderPropagatesMailSenderException() {
        ReflectionTestUtils.setField(service, "mailProvider", "smtp");
        when(provider.getIfAvailable()).thenReturn(mailSender);
        doThrow(new RuntimeException("smtp unavailable")).when(mailSender).send(any(SimpleMailMessage.class));
        assertThatThrownBy(() -> service.sendVerificationOtp("pet@example.com", "123456"))
                .isInstanceOf(RuntimeException.class).hasMessage("smtp unavailable");
    }
}
