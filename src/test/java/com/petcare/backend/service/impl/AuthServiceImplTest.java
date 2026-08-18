package com.petcare.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.petcare.backend.dto.auth.request.*;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.model.*;
import com.petcare.backend.repository.*;
import com.petcare.backend.security.JwtService;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceImplTest {
    @Mock UserRepository users; @Mock UserSocialAccountRepository socialAccounts;
    @Mock PasswordResetTokenRepository resetTokens; @Mock RefreshTokenRepository refreshTokens;
    @Mock UserDeviceRepository devices; @Mock PasswordEncoder encoder;
    @Mock AuthenticationManager authenticationManager; @Mock JwtService jwt;
    @Mock EmailVerificationService verification; @Mock EmailService email;
    @Mock GoogleTokenService google; @Mock org.springframework.beans.factory.ObjectProvider<com.google.firebase.auth.FirebaseAuth> firebaseAuthProvider; AuthServiceImpl service;

    @BeforeEach void setUp() {
        service = new AuthServiceImpl(users, socialAccounts, resetTokens, refreshTokens, devices, encoder,
                authenticationManager, jwt, verification, email, google, firebaseAuthProvider);
        ReflectionTestUtils.setField(service, "refreshTokenExpirationMs", 60_000L);
        ReflectionTestUtils.setField(service, "passwordResetOtpExpirationMinutes", 10L);
        when(refreshTokens.save(any())).thenAnswer(i -> i.getArgument(0));
        when(refreshTokens.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(users.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    }
    private User user(long id) { User u=new User();u.setId(id);u.setEmail("user"+id+"@test.com");u.setFullName("User "+id);u.setStatus("active");u.setEmailVerified(true);return u; }
    private RegisterRequest register(String email) { return RegisterRequest.builder().email(email).password("password1").fullName(" Name ").phoneNumber(" ").build(); }
    private LoginRequest login(String email) { LoginRequest r=new LoginRequest();r.setEmail(email);r.setPassword("password1");return r; }
    private RefreshToken refresh(User u,String value) { RefreshToken t=new RefreshToken();t.setUser(u);t.setToken(value);t.setExpiresAt(LocalDateTime.now().plusMinutes(5));return t; }

    @Test void register_SavesNormalizedEncodedUserAndSendsVerification() {
        User saved=user(1);saved.setEmailVerified(false);when(encoder.encode("password1")).thenReturn("encoded");when(users.save(any())).thenReturn(saved);
        var response=service.register(register(" USER@Test.COM "));
        ArgumentCaptor<User> captured=ArgumentCaptor.forClass(User.class);verify(users).save(captured.capture());
        assertThat(captured.getValue().getEmail()).isEqualTo("user@test.com");assertThat(captured.getValue().getPasswordHash()).isEqualTo("encoded");assertThat(captured.getValue().getPhoneNumber()).isNull();assertThat(response.getUserId()).isEqualTo(1L);verify(verification).createOtp(saved);
    }
    @Test void register_RejectsDuplicateEmailAndPhoneBeforeSave() {
        when(users.existsByEmail("u@test.com")).thenReturn(true);assertThatThrownBy(()->service.register(register("u@test.com"))).isInstanceOf(BadRequestException.class);verifyNoInteractions(encoder,verification);
        when(users.existsByEmail("u@test.com")).thenReturn(false);RegisterRequest r=register("u@test.com");r.setPhoneNumber("0123456789");when(users.existsByPhoneNumber("0123456789")).thenReturn(true);assertThatThrownBy(()->service.register(r)).isInstanceOf(BadRequestException.class);verify(users,never()).save(any());
    }
    @Test void verifyAndResend_VerifyCreatesTokensAndRejectsAlreadyVerified() {
        User u=user(1);u.setEmailVerified(false);when(users.findByEmail("user@test.com")).thenReturn(Optional.of(u));when(users.save(u)).thenReturn(u);when(jwt.generateToken(any())).thenReturn("access");VerifyEmailRequest verifyRequest=new VerifyEmailRequest();verifyRequest.setEmail("USER@test.com");verifyRequest.setOtpCode("123456");
        assertThat(service.verifyEmail(verifyRequest).getAccessToken()).isEqualTo("access");assertThat(u.getEmailVerified()).isTrue();verify(verification).verify(u,"123456");verify(refreshTokens).saveAndFlush(any());
        u.setEmailVerified(true);ResendVerificationRequest resend=new ResendVerificationRequest();resend.setEmail("user@test.com");assertThatThrownBy(()->service.resendVerificationCode(resend)).isInstanceOf(BadRequestException.class);u.setEmailVerified(false);service.resendVerificationCode(resend);verify(verification).createOtp(u);
    }
    @Test void login_SucceedsAndRejectsInactiveOrUnverifiedUsers() {
        User u=user(1);UserPrincipal principal=UserPrincipal.from(u);Authentication auth=mock(Authentication.class);when(auth.getPrincipal()).thenReturn(principal);when(authenticationManager.authenticate(any())).thenReturn(auth);when(users.findByEmail(u.getEmail())).thenReturn(Optional.of(u));when(jwt.generateToken(principal)).thenReturn("access");LoginRequest request=login(u.getEmail());
        assertThat(service.login(request).getAccessToken()).isEqualTo("access");
        u.setEmailVerified(false);assertThatThrownBy(()->service.login(login(u.getEmail()))).isInstanceOf(com.petcare.backend.exception.EmailNotVerifiedException.class);u.setEmailVerified(true);u.setStatus("banned");assertThatThrownBy(()->service.login(login(u.getEmail()))).isInstanceOf(BadRequestException.class);
    }
    @Test void login_RejectsMissingUser() {
        User u=user(1);Authentication auth=mock(Authentication.class);when(auth.getPrincipal()).thenReturn(UserPrincipal.from(u));when(authenticationManager.authenticate(any())).thenReturn(auth);when(users.findByEmail(u.getEmail())).thenReturn(Optional.empty());assertThatThrownBy(()->service.login(login(u.getEmail()))).isInstanceOf(BadRequestException.class);
    }
    @Test void refreshAndLogout_HandleValidRevokedExpiredAndMissingTokens() {
        User u=user(1);RefreshToken token=refresh(u,"good");when(refreshTokens.findByToken("good")).thenReturn(Optional.of(token));when(jwt.generateToken(any())).thenReturn("new-access");RefreshTokenRequest request=new RefreshTokenRequest();request.setRefreshToken("good");assertThat(service.refreshToken(request).getAccessToken()).isEqualTo("new-access");assertThat(token.getRevokedAt()).isNotNull();
        RefreshToken revoked=refresh(u,"revoked");revoked.setRevokedAt(LocalDateTime.now());when(refreshTokens.findByToken("revoked")).thenReturn(Optional.of(revoked));request.setRefreshToken("revoked");assertThatThrownBy(()->service.refreshToken(request)).isInstanceOf(BadRequestException.class);when(refreshTokens.findByToken("missing")).thenReturn(Optional.empty());request.setRefreshToken("missing");assertThatThrownBy(()->service.refreshToken(request)).isInstanceOf(BadRequestException.class);
        LogoutRequest logout=new LogoutRequest();logout.setRefreshToken("good");when(refreshTokens.findByToken("good")).thenReturn(Optional.of(refresh(u,"good")));service.logout(logout);when(refreshTokens.findByToken("none")).thenReturn(Optional.empty());logout.setRefreshToken("none");service.logout(logout);
    }
    @Test void forgotAndResetPassword_InvalidateOldTokensEncodeAndRevokeSessions() {
        User u=user(1);when(users.findByEmail("user@test.com")).thenReturn(Optional.of(u));PasswordResetToken old=new PasswordResetToken();when(resetTokens.findByUserIdAndUsedAtIsNull(1L)).thenReturn(List.of(old));ForgotPasswordRequest forgot=new ForgotPasswordRequest();forgot.setEmail("USER@test.com");service.forgotPassword(forgot);assertThat(old.getUsedAt()).isNotNull();verify(email).sendPasswordResetOtp(eq(u.getEmail()),anyString());
        PasswordResetToken reset=new PasswordResetToken();reset.setUser(u);reset.setExpiresAt(LocalDateTime.now().plusMinutes(1));when(resetTokens.findTopByUserIdAndOtpCodeAndUsedAtIsNullOrderByCreatedAtDesc(1L,"123456")).thenReturn(Optional.of(reset));when(encoder.encode("newpassword")).thenReturn("new-encoded");when(refreshTokens.findByUserIdAndRevokedAtIsNull(1L)).thenReturn(List.of(refresh(u,"one")));ResetPasswordRequest resetRequest=new ResetPasswordRequest();resetRequest.setEmail("user@test.com");resetRequest.setOtpCode("123456");resetRequest.setNewPassword("newpassword");service.resetPassword(resetRequest);assertThat(u.getPasswordHash()).isEqualTo("new-encoded");assertThat(reset.getUsedAt()).isNotNull();
    }
    @Test void resetPassword_RejectsInactiveMissingAndExpiredOtp() {
        User u=user(1);when(users.findByEmail("user@test.com")).thenReturn(Optional.of(u));ResetPasswordRequest r=new ResetPasswordRequest();r.setEmail("user@test.com");r.setOtpCode("123456");r.setNewPassword("password");when(resetTokens.findTopByUserIdAndOtpCodeAndUsedAtIsNullOrderByCreatedAtDesc(1L,"123456")).thenReturn(Optional.empty());assertThatThrownBy(()->service.resetPassword(r)).isInstanceOf(BadRequestException.class);PasswordResetToken expired=new PasswordResetToken();expired.setExpiresAt(LocalDateTime.now().minusSeconds(1));when(resetTokens.findTopByUserIdAndOtpCodeAndUsedAtIsNullOrderByCreatedAtDesc(1L,"123456")).thenReturn(Optional.of(expired));assertThatThrownBy(()->service.resetPassword(r)).isInstanceOf(BadRequestException.class);u.setStatus("disabled");assertThatThrownBy(()->service.resetPassword(r)).isInstanceOf(BadRequestException.class);
    }
    @Test void googleLogin_UsesLinkedAccountAndCreatesNewGoogleUser() {
        GoogleLoginRequest request=new GoogleLoginRequest();request.setIdToken("google");when(google.verify("google")).thenReturn(new GoogleUserPayload("subject"," Google@Test.com ",null,"pic"));User linked=user(2);UserSocialAccount account=new UserSocialAccount();account.setUser(linked);when(socialAccounts.findByProviderAndProviderUserId(any(),eq("subject"))).thenReturn(Optional.of(account));when(jwt.generateToken(any())).thenReturn("access");assertThat(service.loginWithGoogle(request).getAccessToken()).isEqualTo("access");
        when(socialAccounts.findByProviderAndProviderUserId(any(),eq("subject"))).thenReturn(Optional.empty());when(users.findByEmail("google@test.com")).thenReturn(Optional.empty());when(users.save(any())).thenAnswer(i->{User u=i.getArgument(0);u.setId(3L);return u;});assertThat(service.loginWithGoogle(request).getAccessToken()).isEqualTo("access");verify(socialAccounts).save(any());
    }
    @Test void googleLogin_RejectsMissingEmailAndBannedUser() {
        GoogleLoginRequest request=new GoogleLoginRequest();request.setIdToken("bad");when(google.verify("bad")).thenReturn(new GoogleUserPayload("s"," ",null,null));assertThatThrownBy(()->service.loginWithGoogle(request)).isInstanceOf(BadRequestException.class);when(google.verify("bad")).thenReturn(new GoogleUserPayload("s","u@test.com","Name",null));User banned=user(1);banned.setStatus("banned");UserSocialAccount account=new UserSocialAccount();account.setUser(banned);when(socialAccounts.findByProviderAndProviderUserId(any(),eq("s"))).thenReturn(Optional.of(account));assertThatThrownBy(()->service.loginWithGoogle(request)).isInstanceOf(BadRequestException.class);
    }
    @Test void googleExistingUserAndExpiredRefresh_CoverVerificationLinkAndExpiryBranches() {
        GoogleLoginRequest googleRequest=new GoogleLoginRequest();googleRequest.setIdToken("existing");when(google.verify("existing")).thenReturn(new GoogleUserPayload("existing-sub","existing@test.com"," Existing Name ",null));User existing=user(7);existing.setEmailVerified(false);when(socialAccounts.findByProviderAndProviderUserId(any(),eq("existing-sub"))).thenReturn(Optional.empty());when(users.findByEmail("existing@test.com")).thenReturn(Optional.of(existing));when(users.save(existing)).thenReturn(existing);when(jwt.generateToken(any())).thenReturn("access");service.loginWithGoogle(googleRequest);assertThat(existing.getEmailVerified()).isTrue();verify(socialAccounts).save(any());
        RefreshToken expired=refresh(existing,"expired");expired.setExpiresAt(LocalDateTime.now().minusSeconds(1));when(refreshTokens.findByToken("expired")).thenReturn(Optional.of(expired));RefreshTokenRequest refreshRequest=new RefreshTokenRequest();refreshRequest.setRefreshToken("expired");assertThatThrownBy(()->service.refreshToken(refreshRequest)).isInstanceOf(BadRequestException.class);
    }
    @Test void forgotAndReset_WithMissingOrInactiveUserDoNotPersistTokens() {
        ForgotPasswordRequest forgot=new ForgotPasswordRequest();forgot.setEmail("none@test.com");when(users.findByEmail("none@test.com")).thenReturn(Optional.empty());service.forgotPassword(forgot);verifyNoInteractions(email);
        User inactive=user(8);inactive.setStatus("inactive");when(users.findByEmail("inactive@test.com")).thenReturn(Optional.of(inactive));forgot.setEmail("inactive@test.com");service.forgotPassword(forgot);verifyNoInteractions(resetTokens);
        ResetPasswordRequest reset=new ResetPasswordRequest();reset.setEmail("missing@test.com");reset.setOtpCode("123456");reset.setNewPassword("password1");when(users.findByEmail("missing@test.com")).thenReturn(Optional.empty());assertThatThrownBy(()->service.resetPassword(reset)).isInstanceOf(BadRequestException.class);
    }
    @Test void verifyAndResend_RejectUnknownEmailBeforeCallingVerificationService() {
        VerifyEmailRequest verifyRequest=new VerifyEmailRequest();verifyRequest.setEmail("unknown@test.com");verifyRequest.setOtpCode("123456");when(users.findByEmail("unknown@test.com")).thenReturn(Optional.empty());assertThatThrownBy(()->service.verifyEmail(verifyRequest)).isInstanceOf(BadRequestException.class);
        ResendVerificationRequest resend=new ResendVerificationRequest();resend.setEmail("unknown@test.com");assertThatThrownBy(()->service.resendVerificationCode(resend)).isInstanceOf(BadRequestException.class);verifyNoInteractions(verification);
    }
}
