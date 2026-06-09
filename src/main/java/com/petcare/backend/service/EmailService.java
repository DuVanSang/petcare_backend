package com.petcare.backend.service;

import com.petcare.backend.exception.BadRequestException;
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
public class EmailService {
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.mail.provider:log}")
    private String mailProvider;

    @Value("${app.mail.sender-email:}")
    private String senderEmail;

    public void sendVerificationOtp(String toEmail, String otpCode) {
        if ("smtp".equalsIgnoreCase(mailProvider)) {
            sendOtpWithSmtp(toEmail, otpCode);
            return;
        }

        log.info("Email verification OTP for {}: {}", toEmail, otpCode);
    }

    private void sendOtpWithSmtp(String toEmail, String otpCode) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || !StringUtils.hasText(senderEmail)) {
            throw new BadRequestException("SMTP mail chua duoc cau hinh");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(toEmail);
        message.setSubject("PetCare - Ma xac thuc email");
        message.setText("Ma OTP xac thuc email cua ban la: " + otpCode
                + ". Ma co hieu luc trong 10 phut.");
        mailSender.send(message);
    }
}
