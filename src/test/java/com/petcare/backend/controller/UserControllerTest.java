package com.petcare.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.petcare.backend.dto.user.request.ChangePasswordRequest;
import com.petcare.backend.dto.user.request.UpdateProfileRequest;
import com.petcare.backend.dto.user.request.UpdateUserPreferencesRequest;
import com.petcare.backend.dto.user.response.UserResponse;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.UserService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {
    @Mock UserService userService;
    @Mock UserPrincipal principal;
    private UserController controller;

    @BeforeEach void setUp() { controller = new UserController(userService); }

    @Test void allEndpointsDelegateToService() {
        UserResponse response = UserResponse.builder().id(1L).build();
        UpdateProfileRequest profile = new UpdateProfileRequest();
        UpdateUserPreferencesRequest preferences = new UpdateUserPreferencesRequest();
        ChangePasswordRequest password = new ChangePasswordRequest();
        MultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[] {1});
        when(userService.getCurrentUser(principal)).thenReturn(response);
        when(userService.updateProfile(principal, profile)).thenReturn(response);
        when(userService.uploadAvatar(principal, file)).thenReturn(response);
        when(userService.uploadCoverImage(principal, file)).thenReturn(response);
        when(userService.updatePreferences(principal, preferences)).thenReturn(response);
        when(userService.getMyDevices(principal)).thenReturn(List.of());

        assertThat(controller.me(principal).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.updateProfile(principal, profile).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.uploadAvatar(principal, file, null, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.uploadCoverImage(principal, file, null, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.updatePreferences(principal, preferences).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.changePassword(principal, password).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.getMyDevices(principal).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.deleteMyDevice(principal, 3L).getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userService).changePassword(principal, password);
        verify(userService).deleteMyDevice(principal, 3L);
    }

    @Test void uploadImageEndpointsAcceptMobileFieldAliases() {
        UserResponse response = UserResponse.builder().id(1L).build();
        MultipartFile image = new MockMultipartFile("image", "avatar.jpg", "image/jpeg", new byte[] {1});
        when(userService.uploadAvatar(principal, image)).thenReturn(response);
        when(userService.uploadCoverImage(principal, image)).thenReturn(response);

        assertThat(controller.uploadAvatar(principal, null, null, image).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.uploadCoverImage(principal, null, null, image).getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
