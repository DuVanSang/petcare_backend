package com.petcare.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.petcare.backend.dto.auth.request.ResendVerificationRequest;
import com.petcare.backend.dto.auth.request.VerifyEmailRequest;
import com.petcare.backend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    @Mock AuthService authService;
    private AuthController controller;
    private MockMvc mockMvc;

    @BeforeEach void setUp() { controller = new AuthController(authService); mockMvc = MockMvcBuilders.standaloneSetup(controller).build(); }

    @Test void emailVerificationEndpointsDelegateToAuthService() {
        VerifyEmailRequest verifyRequest = new VerifyEmailRequest(); verifyRequest.setEmail("pet@example.com"); verifyRequest.setOtpCode("123456");
        ResendVerificationRequest resendRequest = new ResendVerificationRequest(); resendRequest.setEmail("pet@example.com");
        assertThat(controller.verifyEmail(verifyRequest).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.resendVerificationCode(resendRequest).getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authService).verifyEmail(verifyRequest);
        verify(authService).resendVerificationCode(resendRequest);
    }

    @Test void verificationEndpointsRejectNullBlankAndInvalidEmailOrOtp() throws Exception {
        for (String body : java.util.List.of("{}", "{\"email\":\"bad\",\"otpCode\":\"123\"}", "{\"email\":\"\",\"otpCode\":\"      \"}")) {
            mockMvc.perform(post("/api/v1/auth/verify-email").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(post("/api/v1/auth/resend-verification-code").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"bad\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(authService);
    }
}
