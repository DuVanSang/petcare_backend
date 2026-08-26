package com.petcare.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petcare.backend.dto.admin.user.request.AdminUpdateUserRoleRequest;
import com.petcare.backend.dto.admin.user.request.AdminUpdateUserStatusRequest;
import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.exception.GlobalExceptionHandler;
import com.petcare.backend.exception.ResourceNotFoundException;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.AdminUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {
    @Mock private AdminUserService service;
    @Mock private UserPrincipal principal;
    @InjectMocks private AdminUserController controller;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getUsers_UsesFiltersAndPaginationAndReturnsSuccessJson() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .param("keyword", "ann")
                        .param("role", "USER")
                        .param("status", "ACTIVE")
                        .param("emailVerified", "true")
                        .param("includeDeleted", "true")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyString())));

        verify(service).getUsers("ann", "USER", "ACTIVE", true, true, 0, 1);
    }

    @Test
    void getUsers_AppliesControllerDefaultsWhenFiltersAreMissing() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk());

        verify(service).getUsers(null, null, null, null, false, 0, 20);
    }

    @Test
    void getUserDetail_ReturnsNotFoundResponseWhenServiceThrows() throws Exception {
        when(service.getUserDetail(99L)).thenThrow(new ResourceNotFoundException("Không tìm thấy user"));

        mockMvc.perform(get("/api/v1/admin/users/99"))
                .andExpect(status().isNotFound());

        verify(service).getUserDetail(99L);
    }

    @Test
    void getUserDetail_ReturnsSuccessResponseForExistingUser() {
        ResponseEntity<ApiResponse<?>> response = cast(controller.getUserDetail(7L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).isEqualTo("Lấy chi tiết người dùng thành công");
        verify(service).getUserDetail(7L);
    }

    @Test
    void updateStatusAndRole_DelegateValidRequestsWithCurrentPrincipal() {
        AdminUpdateUserStatusRequest statusRequest = new AdminUpdateUserStatusRequest();
        statusRequest.setStatus("BANNED");
        AdminUpdateUserRoleRequest roleRequest = new AdminUpdateUserRoleRequest();
        roleRequest.setRole("MODERATOR");

        ResponseEntity<ApiResponse<?>> statusResponse = cast(controller.updateUserStatus(principal, 5L, statusRequest));
        ResponseEntity<ApiResponse<?>> roleResponse = cast(controller.updateUserRole(principal, 5L, roleRequest));

        assertThat(statusResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(statusResponse.getBody().isSuccess()).isTrue();
        assertThat(roleResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(roleResponse.getBody().isSuccess()).isTrue();
        verify(service).updateUserStatus(principal, 5L, statusRequest);
        verify(service).updateUserRole(principal, 5L, roleRequest);
    }

    @Test
    void updateStatus_WithBlankBodyFieldReturnsValidationErrorWithoutServiceCall() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/5/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"   \"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void updateRole_WithMalformedJsonReturnsBadRequestWithoutServiceCall() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/5/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ResponseEntity<ApiResponse<?>> cast(ResponseEntity response) {
        return response;
    }
}
