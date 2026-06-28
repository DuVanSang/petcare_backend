package com.petcare.backend.service.impl;

import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.mail.provider:log}")
    private String mailProvider;

    @Value("${app.mail.sender-email:}")
    private String senderEmail;

    @Override
    public void sendVerificationOtp(String toEmail, String otpCode) {
        String subject = "PetCare - Mã xác thực email";
        String body = "Mã OTP xác thực email của bạn là: " + otpCode
                + ". Mã có hiệu lực trong 10 phút.";

        if (isSmtpProvider()) {
            sendWithSmtp(toEmail, subject, body);
            return;
        }

        log.info("Email verification OTP for {}: {}", toEmail, otpCode);
    }

    @Override
    public void sendPasswordResetOtp(String toEmail, String otpCode) {
        String subject = "PetCare - Mã đặt lại mật khẩu";
        String body = "Mã OTP đặt lại mật khẩu của bạn là: " + otpCode
                + ". Mã có hiệu lực trong 10 phút.";

        if (isSmtpProvider()) {
            sendWithSmtp(toEmail, subject, body);
            return;
        }

        log.info("Password reset OTP for {}: {}", toEmail, otpCode);
    }

    private boolean isSmtpProvider() {
        return "smtp".equalsIgnoreCase(mailProvider);
    }

    private void sendWithSmtp(String toEmail, String subject, String body) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || !StringUtils.hasText(senderEmail)) {
            throw new BadRequestException("SMTP mail chưa được cấu hình");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
