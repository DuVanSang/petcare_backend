package com.petcare.backend.service;

import com.petcare.backend.exception.BadRequestException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class GoogleTokenService {
    private final String clientId;
    private final RestClient restClient;

    public GoogleTokenService(@Value("${app.google.client-id}") String clientId) {
        this.clientId = clientId;
        this.restClient = RestClient.create();
    }

    @SuppressWarnings("unchecked")
    public GoogleUserPayload verify(String idToken) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri("https://oauth2.googleapis.com/tokeninfo?id_token={token}", idToken)
                    .retrieve()
                    .body(Map.class);

            if (response == null || response.containsKey("error")) {
                throw new BadRequestException("Google token không hợp lệ");
            }

            if (!clientId.equals(response.get("aud"))) {
                throw new BadRequestException("Google token không hợp lệ");
            }

            String email = (String) response.get("email");
            String subject = (String) response.get("sub");
            if (email == null || subject == null) {
                throw new BadRequestException("Google token không hợp lệ");
            }

            return new GoogleUserPayload(
                    subject,
                    email,
                    (String) response.get("name"),
                    (String) response.get("picture")
            );
        } catch (BadRequestException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new BadRequestException("Không thể xác thực Google token");
        }
    }
}
