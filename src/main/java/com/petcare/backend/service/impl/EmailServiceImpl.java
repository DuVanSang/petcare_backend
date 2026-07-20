package com.petcare.backend.service.impl;

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
    public boolean sendVerificationOtp(String toEmail, String otpCode) {
        String subject = "PetCare - Mã xác thực email";
        String body = "Mã OTP xác thực email của bạn là: " + otpCode
                + ". Mã có hiệu lực trong 10 phút.";

        return deliverEmail(toEmail, subject, body);
    }

    @Override
    public boolean sendPasswordResetOtp(String toEmail, String otpCode) {
        String subject = "PetCare - Mã đặt lại mật khẩu";
        String body = "Mã OTP đặt lại mật khẩu của bạn là: " + otpCode
                + ". Mã có hiệu lực trong 10 phút.";

        return deliverEmail(toEmail, subject, body);
    }

    private boolean deliverEmail(String toEmail, String subject, String body) {
        if (!isSmtpProvider()) {
            log.info("Email OTP for {}: {}", toEmail, body);
            return false;
        }

        return sendWithSmtp(toEmail, subject, body);
    }

    private boolean isSmtpProvider() {
        return "smtp".equalsIgnoreCase(mailProvider);
    }

    private boolean sendWithSmtp(String toEmail, String subject, String body) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || !StringUtils.hasText(senderEmail)) {
            log.warn("SMTP chưa cấu hình, in OTP ra log cho {}", toEmail);
            log.info("Email fallback for {}: {}", toEmail, body);
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Đã gửi email OTP thành công cho {}", toEmail);
            return true;
        } catch (org.springframework.mail.MailException ex) {
            log.error("Không gửi được email qua SMTP cho {}, in OTP ra log", toEmail, ex);
            log.info("Email fallback for {}: {}", toEmail, body);
            return false;
        }
    }
}
