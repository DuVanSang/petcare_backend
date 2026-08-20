package com.petcare.backend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petcare.backend.service.EmailService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EmailServiceImpl(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    @Value("${app.mail.provider:resend}")
    private String mailProvider;

    @Value("${app.mail.sender-email:}")
    private String senderEmail;

    @Value("${app.mail.resend.api-key:}")
    private String resendApiKey;

    @Value("${app.mail.resend.from-email:PetCare <onboarding@resend.dev>}")
    private String resendFromEmail;

    @Override
    public boolean sendVerificationOtp(String toEmail, String otpCode) {
        String subject = "PetCare - Mã xác thực email";
        String body = "Mã OTP xác thực email của bạn là: " + otpCode
                + ". Mã có hiệu lực trong 10 phút.";

        return deliverEmail(toEmail, subject, body, otpCode);
    }

    @Override
    public boolean sendPasswordResetOtp(String toEmail, String otpCode) {
        String subject = "PetCare - Mã đặt lại mật khẩu";
        String body = "Mã OTP đặt lại mật khẩu của bạn là: " + otpCode
                + ". Mã có hiệu lực trong 10 phút.";

        return deliverEmail(toEmail, subject, body, otpCode);
    }

    private boolean deliverEmail(String toEmail, String subject, String body, String otpCode) {
        String provider = StringUtils.hasText(mailProvider) ? mailProvider.trim().toLowerCase() : "resend";

        if ("resend".equalsIgnoreCase(provider)) {
            return sendWithResend(toEmail, subject, body, otpCode);
        } else if ("smtp".equalsIgnoreCase(provider)) {
            return sendWithSmtp(toEmail, subject, body, otpCode);
        } else {
            log.info("Email OTP for {}: {}", toEmail, body);
            return false;
        }
    }

    /**
     * Gửi email OTP qua Resend REST API (HTTPS Port 443 - không bị chặn trên bất kỳ VPS nào).
     */
    private boolean sendWithResend(String toEmail, String subject, String body, String otpCode) {
        if (!StringUtils.hasText(resendApiKey)) {
            log.warn("RESEND_API_KEY chưa được cấu hình, in OTP ra log cho {}", toEmail);
            log.info("Email fallback for {}: {}", toEmail, body);
            return false;
        }

        try {
            String from = StringUtils.hasText(resendFromEmail) ? resendFromEmail : "PetCare <onboarding@resend.dev>";
            String htmlContent = buildOtpHtml(subject, otpCode);

            Map<String, Object> payload = new HashMap<>();
            payload.put("from", from);
            payload.put("to", Collections.singletonList(toEmail));
            payload.put("subject", subject);
            payload.put("html", htmlContent);
            payload.put("text", body);

            String jsonBody = objectMapper.writeValueAsString(payload);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + resendApiKey.trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Đã gửi email OTP qua Resend thành công cho {} (status={})", toEmail, response.statusCode());
                return true;
            } else {
                log.error("Resend API trả về lỗi (status={}): {}", response.statusCode(), response.body());
                log.info("Email fallback for {}: {}", toEmail, body);
                return false;
            }
        } catch (Exception ex) {
            log.error("Không thể gửi email qua Resend cho {}", toEmail, ex);
            log.info("Email fallback for {}: {}", toEmail, body);
            return false;
        }
    }

    /**
     * Gửi email OTP qua SMTP JavaMailSender truyền thống.
     */
    private boolean sendWithSmtp(String toEmail, String subject, String body, String otpCode) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || !StringUtils.hasText(senderEmail)) {
            log.warn("SMTP chưa cấu hình, in OTP ra log cho {}", toEmail);
            log.info("Email fallback for {}: {}", toEmail, body);
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(senderEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body, buildOtpHtml(subject, otpCode));
            mailSender.send(message);
            log.info("Đã gửi email OTP qua SMTP thành công cho {}", toEmail);
            return true;
        } catch (MessagingException | MailException ex) {
            log.error("Không gửi được email qua SMTP cho {}, in OTP ra log", toEmail, ex);
            log.info("Email fallback for {}: {}", toEmail, body);
            return false;
        }
    }

    /**
     * Tạo giao diện HTML Email chuyên nghiệp cho mã OTP.
     */
    private String buildOtpHtml(String title, String otpCode) {
        return "<div style=\"font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 520px; margin: 0 auto; padding: 28px; border: 1px solid #e2e8f0; border-radius: 16px; background-color: #ffffff;\">"
                + "<div style=\"text-align: center; margin-bottom: 24px;\">"
                + "<h2 style=\"color: #2563eb; margin: 0; font-size: 24px;\">🐾 PetCare</h2>"
                + "<p style=\"color: #64748b; font-size: 13px; margin: 4px 0 0;\">Hệ thống chăm sóc thú cưng thông minh</p>"
                + "</div>"
                + "<div style=\"background: linear-gradient(135deg, #f8fafc, #f1f5f9); padding: 24px; border-radius: 14px; text-align: center; border: 1px solid #e2e8f0; margin-bottom: 20px;\">"
                + "<p style=\"color: #334155; font-size: 15px; font-weight: 600; margin: 0 0 14px;\">" + title + "</p>"
                + "<div style=\"font-size: 34px; font-weight: 800; letter-spacing: 8px; color: #1e40af; font-family: monospace; background: #e0e7ff; padding: 12px 24px; border-radius: 10px; display: inline-block; box-shadow: 0 2px 8px rgba(30, 64, 175, 0.15);\">"
                + otpCode
                + "</div>"
                + "<p style=\"color: #ef4444; font-size: 13px; font-weight: 500; margin: 14px 0 0;\">⏳ Mã có hiệu lực trong vòng 10 phút.</p>"
                + "</div>"
                + "<p style=\"color: #64748b; font-size: 13px; line-height: 1.5; margin: 0 0 16px;\">Vui lòng không chia sẻ mã này cho bất kỳ ai để đảm bảo an toàn cho tài khoản của bạn.</p>"
                + "<hr style=\"border: none; border-top: 1px solid #f1f5f9; margin: 20px 0;\" />"
                + "<p style=\"color: #94a3b8; font-size: 11.5px; text-align: center; margin: 0;\">© 2026 PetCare Ecosystem. All rights reserved.</p>"
                + "</div>";
    }
}
