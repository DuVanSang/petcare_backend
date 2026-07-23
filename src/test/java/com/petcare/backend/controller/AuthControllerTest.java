package com.petcare.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.petcare.backend.dto.auth.request.ResendVerificationRequest;
import com.petcare.backend.dto.auth.request.VerifyEmailRequest;
import com.petcare.backend.dto.auth.request.*;
import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.exception.GlobalExceptionHandler;
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

    @BeforeEach void setUp() { controller = new AuthController(authService); mockMvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build(); }

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

    @Test void remainingAuthEndpoints_ReturnExpectedSuccessResponsesAndDelegateRequests() {
        RegisterRequest register = RegisterRequest.builder().email("pet@example.com").password("password1").fullName("Pet Owner").phoneNumber("0123456789").build();
        LoginRequest login = new LoginRequest(); login.setEmail("pet@example.com"); login.setPassword("password1");
        GoogleLoginRequest google = new GoogleLoginRequest(); google.setIdToken("google-token");
        RefreshTokenRequest refresh = new RefreshTokenRequest(); refresh.setRefreshToken("refresh-token");
        LogoutRequest logout = new LogoutRequest(); logout.setRefreshToken("refresh-token");
        ForgotPasswordRequest forgot = new ForgotPasswordRequest(); forgot.setEmail("pet@example.com");
        ResetPasswordRequest reset = new ResetPasswordRequest(); reset.setEmail("pet@example.com"); reset.setOtpCode("123456"); reset.setNewPassword("password1");

        assertThat(controller.register(register).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.login(login).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.loginWithGoogle(google).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.refreshToken(refresh).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.logout(logout).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.forgotPassword(forgot).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.resetPassword(reset).getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authService).register(register); verify(authService).login(login); verify(authService).loginWithGoogle(google);
        verify(authService).refreshToken(refresh); verify(authService).logout(logout); verify(authService).forgotPassword(forgot); verify(authService).resetPassword(reset);
    }

    @Test void authRequests_RejectInvalidEmailTokenAndPasswordWithoutCallingService() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bad\",\"password\":\"short\",\"fullName\":\" \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/auth/refresh-token").contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\" \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"otpCode\":\"12\",\"newPassword\":\"short\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/auth/google").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(authService);
    }

    @Test void register_MapsBusinessExceptionToBadRequestJson() throws Exception {
        org.mockito.Mockito.when(authService.register(org.mockito.ArgumentMatchers.any(RegisterRequest.class)))
                .thenThrow(new BadRequestException("Email đã tồn tại"));

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"pet@example.com\",\"password\":\"password1\",\"fullName\":\"Pet Owner\",\"phoneNumber\":\"0123456789\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.success").value(false))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message").value("Email đã tồn tại"));
    }
}
