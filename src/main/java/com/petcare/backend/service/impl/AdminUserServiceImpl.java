package com.petcare.backend.service.impl;

import com.petcare.backend.dto.admin.user.request.AdminUpdateUserRoleRequest;
import com.petcare.backend.dto.admin.user.request.AdminUpdateUserStatusRequest;
import com.petcare.backend.dto.admin.user.response.AdminUserDetailResponse;
import com.petcare.backend.dto.admin.user.response.AdminUserResponse;
import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.dto.user.response.UserDeviceResponse;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.exception.ResourceNotFoundException;
import com.petcare.backend.model.User;
import com.petcare.backend.repository.UserDeviceRepository;
import com.petcare.backend.repository.UserRepository;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.AdminUserService;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_ROLES = Set.of("user", "admin", "moderator");
    private static final Set<String> ALLOWED_STATUSES = Set.of("active", "banned");

    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> getUsers(
            String keyword,
            String role,
            String status,
            Boolean emailVerified,
            Boolean includeDeleted,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(
                validatePage(page),
                validateSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<AdminUserResponse> users = userRepository
                .findAll(buildSpecification(keyword, role, status, emailVerified, includeDeleted), pageable)
                .map(AdminUserResponse::from);
        return PageResponse.from(users);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUserDetail(Long userId) {
        return toDetailResponse(getUserOrThrow(userId));
    }

    @Override
    @Transactional
    public AdminUserDetailResponse updateUserStatus(
            UserPrincipal currentAdmin,
            Long userId,
            AdminUpdateUserStatusRequest request
    ) {
        User user = getUserOrThrow(userId);
        String newStatus = normalizeAllowedValue(request.getStatus(), ALLOWED_STATUSES, "Trạng thái người dùng không hợp lệ");

        if (currentAdmin.getId().equals(user.getId()) && "banned".equals(newStatus)) {
            throw new BadRequestException("Admin không thể tự khóa tài khoản của mình");
        }

        if (isActiveAdmin(user) && !"active".equals(newStatus)) {
            ensureMoreThanOneActiveAdmin();
        }

        user.setStatus(newStatus);
        return toDetailResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public AdminUserDetailResponse updateUserRole(
            UserPrincipal currentAdmin,
            Long userId,
            AdminUpdateUserRoleRequest request
    ) {
        User user = getUserOrThrow(userId);
        String newRole = normalizeAllowedValue(request.getRole(), ALLOWED_ROLES, "Vai trò người dùng không hợp lệ");

        if (currentAdmin.getId().equals(user.getId()) && !"admin".equals(newRole)) {
            throw new BadRequestException("Admin không thể tự hạ quyền của mình");
        }

        if (isActiveAdmin(user) && !"admin".equals(newRole)) {
            ensureMoreThanOneActiveAdmin();
        }

        user.setRole(newRole);
        return toDetailResponse(userRepository.save(user));
    }

    private Specification<User> buildSpecification(
            String keyword,
            String role,
            String status,
            Boolean emailVerified,
            Boolean includeDeleted
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!Boolean.TRUE.equals(includeDeleted)) {
                predicates.add(cb.isNull(root.get("deletedAt")));
            }

            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(root.get("fullName")), pattern),
                        cb.like(cb.lower(root.get("username")), pattern),
                        cb.like(cb.lower(root.get("phoneNumber")), pattern),
                        cb.like(cb.lower(root.get("bio")), pattern),
                        cb.like(cb.lower(root.get("location")), pattern)
                ));
            }

            if (StringUtils.hasText(role)) {
                predicates.add(cb.equal(root.get("role"), normalizeAllowedValue(
                        role,
                        ALLOWED_ROLES,
                        "Vai trò người dùng không hợp lệ"
                )));
            }

            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), normalizeAllowedValue(
                        status,
                        ALLOWED_STATUSES,
                        "Trạng thái người dùng không hợp lệ"
                )));
            }

            if (emailVerified != null) {
                predicates.add(cb.equal(root.get("emailVerified"), emailVerified));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private AdminUserDetailResponse toDetailResponse(User user) {
        List<UserDeviceResponse> devices = userDeviceRepository.findByUserId(user.getId())
                .stream()
                .map(UserDeviceResponse::from)
                .toList();
        return AdminUserDetailResponse.from(user, devices);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
    }

    private String normalizeAllowedValue(String value, Set<String> allowedValues, String errorMessage) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException(errorMessage);
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!allowedValues.contains(normalized)) {
            throw new BadRequestException(errorMessage);
        }
        return normalized;
    }

    private int validatePage(int page) {
        if (page < 0) {
            throw new BadRequestException("Số trang không được âm");
        }
        return page;
    }

    private int validateSize(int size) {
        if (size <= 0) {
            throw new BadRequestException("Kích thước trang phải lớn hơn 0");
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private boolean isActiveAdmin(User user) {
        return "admin".equalsIgnoreCase(user.getRole()) && "active".equalsIgnoreCase(user.getStatus());
    }

    private void ensureMoreThanOneActiveAdmin() {
        if (userRepository.countByRoleAndStatusAndDeletedAtIsNull("admin", "active") <= 1) {
            throw new BadRequestException("Hệ thống phải còn ít nhất một admin đang hoạt động");
        }
    }
}
