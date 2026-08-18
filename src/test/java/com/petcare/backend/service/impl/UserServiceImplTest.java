package com.petcare.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.petcare.backend.dto.upload.UploadFileResponse;
import com.petcare.backend.dto.user.request.ChangePasswordRequest;
import com.petcare.backend.dto.user.request.UpdateProfileRequest;
import com.petcare.backend.dto.user.request.UpdateUserPreferencesRequest;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.exception.ResourceNotFoundException;
import com.petcare.backend.model.User;
import com.petcare.backend.model.UserDevice;
import com.petcare.backend.repository.UserDeviceRepository;
import com.petcare.backend.repository.UserRepository;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.FileStorageService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class UserServiceImplTest {
    @Mock UserRepository users;
    @Mock UserDeviceRepository devices;
    @Mock PasswordEncoder passwords;
    @Mock FileStorageService files;
    @Mock UserPrincipal principal;
    @Mock MultipartFile file;
    private UserServiceImpl service;

    @BeforeEach void setUp() {
        service = new UserServiceImpl(users, devices, passwords, files);
        when(principal.getId()).thenReturn(1L);
        when(users.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
    }

    private User user() {
        User user = new User(); user.setId(1L); user.setEmail("a@pet.test"); user.setFullName("Before");
        user.setPasswordHash("hash"); user.setLanguageCode("vi"); user.setTimezone("Asia/Ho_Chi_Minh");
        user.setPushNotificationEnabled(true); return user;
    }

    private void current(User user) { when(users.findById(1L)).thenReturn(Optional.of(user)); }

    @Test void getCurrentUser_mapsExistingUserAndThrowsWhenMissing() {
        current(user());
        assertThat(service.getCurrentUser(principal).getEmail()).isEqualTo("a@pet.test");
        when(users.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getCurrentUser(principal)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test void updateProfile_updatesAndTrimsAllProvidedFields() {
        User user = user(); current(user);
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("  Alice  "); request.setUsername("  alice_1  "); request.setBio("  pet parent  ");
        request.setDateOfBirth(LocalDate.now().minusDays(1)); request.setLocation("  Hanoi  "); request.setPhoneNumber(" 0123456789 ");
        request.setAvatarUrl("  avatar "); request.setCoverImageUrl(" cover ");
        when(users.existsByUsernameIgnoreCaseAndIdNot("alice_1", 1L)).thenReturn(false);

        var result = service.updateProfile(principal, request);

        assertThat(result.getFullName()).isEqualTo("Alice"); assertThat(user.getUsername()).isEqualTo("alice_1");
        assertThat(user.getBio()).isEqualTo("pet parent"); assertThat(user.getLocation()).isEqualTo("Hanoi");
        assertThat(user.getPhoneNumber()).isEqualTo("0123456789"); assertThat(user.getAvatarUrl()).isEqualTo("avatar");
        assertThat(user.getCoverImageUrl()).isEqualTo("cover"); verify(users).save(user);
    }

    @Test void updateProfile_handlesEmptyOptionalValuesDuplicateUsernameAndMissingUser() {
        User user = user(); current(user);
        UpdateProfileRequest blank = new UpdateProfileRequest();
        blank.setFullName("   "); blank.setUsername("   "); blank.setBio(""); blank.setLocation(" ");
        blank.setPhoneNumber(""); blank.setAvatarUrl(""); blank.setCoverImageUrl(" ");
        service.updateProfile(principal, blank);
        assertThat(user.getFullName()).isEqualTo("Before"); assertThat(user.getUsername()).isNull(); assertThat(user.getBio()).isNull();
        assertThat(user.getLocation()).isNull(); assertThat(user.getPhoneNumber()).isNull(); assertThat(user.getAvatarUrl()).isNull();
        service.updateProfile(principal, new UpdateProfileRequest());
        verify(users, times(2)).save(user);

        UpdateProfileRequest duplicate = new UpdateProfileRequest(); duplicate.setUsername("taken");
        when(users.existsByUsernameIgnoreCaseAndIdNot("taken", 1L)).thenReturn(true);
        assertThatThrownBy(() -> service.updateProfile(principal, duplicate)).isInstanceOf(BadRequestException.class);
        when(users.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateProfile(principal, new UpdateProfileRequest())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test void avatarAndCoverUploadReplaceUrlsDeletePreviousAndPropagateMissingUser() {
        User user = user(); user.setAvatarUrl("old-avatar"); user.setCoverImageUrl("old-cover"); current(user);
        when(files.storeUserProfileImage(file, 1L, "avatar")).thenReturn(UploadFileResponse.builder().mediaUrl("new-avatar").build());
        when(files.storeUserProfileImage(file, 1L, "cover")).thenReturn(UploadFileResponse.builder().mediaUrl("new-cover").build());
        assertThat(service.uploadAvatar(principal, file).getAvatarUrl()).isEqualTo("new-avatar");
        assertThat(service.uploadCoverImage(principal, file).getCoverImageUrl()).isEqualTo("new-cover");
        verify(files).deleteByUrl("old-avatar"); verify(files).deleteByUrl("old-cover");
        when(users.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.uploadAvatar(principal, file)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test void preferences_coverProvidedAndAbsentValues() {
        User user = user(); current(user);
        UpdateUserPreferencesRequest set = new UpdateUserPreferencesRequest();
        set.setLanguageCode(" en "); set.setTimezone(" UTC "); set.setPushNotificationEnabled(false);
        service.updatePreferences(principal, set);
        assertThat(user.getLanguageCode()).isEqualTo("en"); assertThat(user.getTimezone()).isEqualTo("UTC"); assertThat(user.getPushNotificationEnabled()).isFalse();
        UpdateUserPreferencesRequest absent = new UpdateUserPreferencesRequest(); absent.setLanguageCode(" "); absent.setTimezone("");
        service.updatePreferences(principal, absent);
        assertThat(user.getLanguageCode()).isEqualTo("en"); assertThat(user.getTimezone()).isEqualTo("UTC");
    }

    @Test void changePassword_coversWrongCurrentRepeatedNewAndSuccessfulChange() {
        User user = user(); current(user);
        ChangePasswordRequest request = new ChangePasswordRequest(); request.setCurrentPassword("old"); request.setNewPassword("new-password");
        when(passwords.matches("old", "hash")).thenReturn(false);
        assertThatThrownBy(() -> service.changePassword(principal, request)).isInstanceOf(BadRequestException.class);
        when(passwords.matches("old", "hash")).thenReturn(true); when(passwords.matches("new-password", "hash")).thenReturn(true);
        assertThatThrownBy(() -> service.changePassword(principal, request)).isInstanceOf(BadRequestException.class);
        when(passwords.matches("new-password", "hash")).thenReturn(false); when(passwords.encode("new-password")).thenReturn("new-hash");
        service.changePassword(principal, request);
        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
    }

    @Test void devices_mapEmptyAndPresentListsAndDeleteOnlyOwnedDevice() {
        when(devices.findByUserId(1L)).thenReturn(List.of());
        assertThat(service.getMyDevices(principal)).isEmpty();
        UserDevice device = new UserDevice(); device.setId(2L); device.setDeviceId("phone"); device.setDeviceType("android");
        when(devices.findByUserId(1L)).thenReturn(List.of(device));
        assertThat(service.getMyDevices(principal)).extracting("deviceId").containsExactly("phone");
        when(devices.findByIdAndUserId(2L, 1L)).thenReturn(Optional.of(device));
        service.deleteMyDevice(principal, 2L); verify(devices).delete(device);
        when(devices.findByIdAndUserId(3L, 1L)).thenReturn(Optional.empty());
        service.deleteMyDevice(principal, 3L); verify(devices, times(1)).delete(any());
    }
}
