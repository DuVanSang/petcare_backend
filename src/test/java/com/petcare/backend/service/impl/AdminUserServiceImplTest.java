package com.petcare.backend.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petcare.backend.dto.admin.user.request.AdminUpdateUserRoleRequest;
import com.petcare.backend.dto.admin.user.request.AdminUpdateUserStatusRequest;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.exception.ResourceNotFoundException;
import com.petcare.backend.model.User;
import com.petcare.backend.repository.UserDeviceRepository;
import com.petcare.backend.repository.UserRepository;
import com.petcare.backend.security.UserPrincipal;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings({"rawtypes", "unchecked"})
class AdminUserServiceImplTest {
    @Mock private UserRepository users;
    @Mock private UserDeviceRepository devices;
    @Mock private UserPrincipal principal;
    private AdminUserServiceImpl service;

    @BeforeEach void setUp() { service = new AdminUserServiceImpl(users, devices); }

    @Test
    void getUsersMapsDataAppliesAllFiltersAndCapsSize() {
        when(users.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(user(2L, "user", "active"))));
        var result = service.getUsers("  Alice ", " USER ", " ACTIVE ", true, false, 0, 101);
        assertEquals(1, result.getContent().size()); assertEquals(2L, result.getContent().getFirst().getId());
        assertEquals("user", result.getContent().getFirst().getRole());
        ArgumentCaptor<Specification> spec = ArgumentCaptor.forClass(Specification.class); ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(users).findAll(spec.capture(), pageable.capture()); assertEquals(100, pageable.getValue().getPageSize());
        assertEquals("createdAt: DESC", pageable.getValue().getSort().toString()); executeSpecification(spec.getValue());
    }

    @Test
    void getUsersSupportsNoFiltersEmptyPageAndValidationBoundaries() {
        when(users.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        assertEquals(0, service.getUsers(" ", null, null, null, true, 0, 1).getContent().size());
        ArgumentCaptor<Specification> spec = ArgumentCaptor.forClass(Specification.class); verify(users).findAll(spec.capture(), any(Pageable.class)); executeSpecification(spec.getValue());
        assertThrows(BadRequestException.class, () -> service.getUsers(null, null, null, null, null, -1, 1));
        assertThrows(BadRequestException.class, () -> service.getUsers(null, null, null, null, null, 0, 0));
    }

    @Test
    void getUserDetailMapsUserAndThrowsWhenUserIsMissing() {
        User user = user(3L, "user", "active"); when(users.findById(3L)).thenReturn(Optional.of(user)); when(devices.findByUserId(3L)).thenReturn(List.of());
        var detail = service.getUserDetail(3L); assertEquals(3L, detail.getId()); assertEquals("user@example.com", detail.getEmail()); assertEquals("user", detail.getRole());
        when(users.findById(9L)).thenReturn(Optional.empty()); assertThrows(ResourceNotFoundException.class, () -> service.getUserDetail(9L));
    }

    @Test
    void updateUserStatusNormalizesAndSavesRegularUser() {
        User user = user(4L, "user", "active"); when(users.findById(4L)).thenReturn(Optional.of(user)); when(users.save(user)).thenReturn(user); when(devices.findByUserId(4L)).thenReturn(List.of()); when(principal.getId()).thenReturn(1L);
        AdminUpdateUserStatusRequest request = new AdminUpdateUserStatusRequest(); request.setStatus(" BANNED ");
        var detail = service.updateUserStatus(principal, 4L, request);
        assertEquals("banned", user.getStatus()); assertEquals("banned", detail.getStatus()); verify(users).save(user);
    }

    @Test
    void updateUserStatusRejectsInvalidSelfBanAndLastActiveAdmin() {
        User self = user(1L, "admin", "active"); when(users.findById(1L)).thenReturn(Optional.of(self)); when(principal.getId()).thenReturn(1L);
        assertThrows(BadRequestException.class, () -> service.updateUserStatus(principal, 1L, statusRequest("banned")));
        User admin = user(2L, "admin", "active"); when(users.findById(2L)).thenReturn(Optional.of(admin)); when(principal.getId()).thenReturn(1L); when(users.countByRoleAndStatusAndDeletedAtIsNull("admin", "active")).thenReturn(1L);
        assertThrows(BadRequestException.class, () -> service.updateUserStatus(principal, 2L, statusRequest("banned")));
        assertThrows(BadRequestException.class, () -> service.updateUserStatus(principal, 2L, statusRequest("unknown")));
        assertThrows(BadRequestException.class, () -> service.updateUserStatus(principal, 2L, statusRequest(" ")));
    }

    @Test
    void updateUserRoleProtectsAdminsAndAllowsChangeWhenAnotherAdminExists() {
        User self = user(1L, "admin", "active"); when(users.findById(1L)).thenReturn(Optional.of(self)); when(principal.getId()).thenReturn(1L);
        assertThrows(BadRequestException.class, () -> service.updateUserRole(principal, 1L, roleRequest("moderator")));
        User admin = user(2L, "admin", "active"); when(users.findById(2L)).thenReturn(Optional.of(admin)); when(users.countByRoleAndStatusAndDeletedAtIsNull("admin", "active")).thenReturn(2L); when(users.save(admin)).thenReturn(admin); when(devices.findByUserId(2L)).thenReturn(List.of());
        var detail = service.updateUserRole(principal, 2L, roleRequest(" MODERATOR "));
        assertEquals("moderator", admin.getRole()); assertEquals("moderator", detail.getRole()); verify(users).save(admin);
        assertThrows(BadRequestException.class, () -> service.updateUserRole(principal, 2L, roleRequest("invalid")));
        assertThrows(BadRequestException.class, () -> service.updateUserRole(principal, 2L, roleRequest(" ")));
    }

    private void executeSpecification(Specification specification) { specification.toPredicate(root(), mock(CriteriaQuery.class), criteriaBuilder()); }
    private Root root() { Root root = mock(Root.class); Path path = path(); when(root.get(any(String.class))).thenReturn(path); return root; }
    private Path path() { Path path = mock(Path.class); return path; }
    private CriteriaBuilder criteriaBuilder() { CriteriaBuilder cb = mock(CriteriaBuilder.class); Predicate predicate = mock(Predicate.class); when(cb.isNull(any())).thenReturn(predicate); when(cb.lower(any())).thenReturn(mock(jakarta.persistence.criteria.Expression.class)); when(cb.like(any(), any(String.class))).thenReturn(predicate); when(cb.or(any(Predicate[].class))).thenReturn(predicate); when(cb.equal(any(), any())).thenReturn(predicate); when(cb.and(any(Predicate[].class))).thenReturn(predicate); return cb; }
    private User user(Long id, String role, String status) { User user = new User(); user.setId(id); user.setEmail("user@example.com"); user.setFullName("User"); user.setRole(role); user.setStatus(status); user.setEmailVerified(true); return user; }
    private AdminUpdateUserStatusRequest statusRequest(String status) { AdminUpdateUserStatusRequest request = new AdminUpdateUserStatusRequest(); request.setStatus(status); return request; }
    private AdminUpdateUserRoleRequest roleRequest(String role) { AdminUpdateUserRoleRequest request = new AdminUpdateUserRoleRequest(); request.setRole(role); return request; }
}
