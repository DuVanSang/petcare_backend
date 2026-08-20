package com.petcare.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
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

    @Test void smtpProviderSendsVerificationAndResetMessagesWithExpectedArguments() throws Exception {
        ReflectionTestUtils.setField(service, "mailProvider", "smtp");
        when(provider.getIfAvailable()).thenReturn(mailSender);
        when(mailSender.createMimeMessage())
                .thenReturn(createMimeMessage(), createMimeMessage());

        service.sendVerificationOtp("pet@example.com", "123456");
        service.sendPasswordResetOtp("pet@example.com", "654321");

        ArgumentCaptor<MimeMessage> messages = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(2)).send(messages.capture());
        MimeMessage verification = messages.getAllValues().get(0);
        MimeMessage reset = messages.getAllValues().get(1);
        assertThat(verification.getFrom()[0].toString()).contains("noreply@petcare.test");
        assertThat(verification.getAllRecipients()[0].toString()).isEqualTo("pet@example.com");
        assertThat(verification.getSubject()).contains("xác thực");
        assertThat(rawMessage(verification)).contains("123456");
        assertThat(reset.getSubject()).contains("đặt lại");
        assertThat(rawMessage(reset)).contains("654321");
    }

    @Test void smtpProviderPropagatesMailSenderException() {
        ReflectionTestUtils.setField(service, "mailProvider", "smtp");
        when(provider.getIfAvailable()).thenReturn(mailSender);
        doThrow(new RuntimeException("smtp unavailable")).when(mailSender).createMimeMessage();
        assertThatThrownBy(() -> service.sendVerificationOtp("pet@example.com", "123456"))
                .isInstanceOf(RuntimeException.class).hasMessage("smtp unavailable");
    }

    private static MimeMessage createMimeMessage() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }

    private static String rawMessage(MimeMessage message) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            message.writeTo(output);
            return output.toString(StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }
}
