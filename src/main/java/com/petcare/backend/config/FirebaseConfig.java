package com.petcare.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@ConditionalOnProperty(name = "app.firebase.enabled", havingValue = "true")
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp(
            @Value("${app.firebase.service-account-path:}") String serviceAccountPath,
            @Value("${app.firebase.service-account-json-base64:}") String serviceAccountJsonBase64
    ) throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        GoogleCredentials credentials = loadCredentials(serviceAccountPath, serviceAccountJsonBase64);
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();
        return FirebaseApp.initializeApp(options);
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }

    private GoogleCredentials loadCredentials(String serviceAccountPath, String serviceAccountJsonBase64)
            throws IOException {
        if (StringUtils.hasText(serviceAccountJsonBase64)) {
            byte[] decoded = Base64.getDecoder().decode(serviceAccountJsonBase64.trim());
            return GoogleCredentials.fromStream(new ByteArrayInputStream(decoded));
        }

        if (StringUtils.hasText(serviceAccountPath)) {
            return GoogleCredentials.fromStream(new FileInputStream(serviceAccountPath.trim()));
        }

        throw new IllegalStateException(
                "Firebase đã được bật nhưng chưa cấu hình service account. "
                        + "Hãy khai báo FIREBASE_SERVICE_ACCOUNT_PATH hoặc FIREBASE_SERVICE_ACCOUNT_JSON_BASE64."
        );
    }
}
