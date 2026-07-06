package com.petcare.backend.service.impl;

import com.petcare.backend.dto.admin.reminder.response.AdminReminderLogResponse;
import com.petcare.backend.dto.admin.reminder.response.AdminVaccinationReminderLogResponse;
import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.exception.ResourceNotFoundException;
import com.petcare.backend.model.CareReminder;
import com.petcare.backend.model.CareReminderLog;
import com.petcare.backend.model.VaccinationReminderLog;
import com.petcare.backend.repository.CareReminderLogRepository;
import com.petcare.backend.repository.VaccinationReminderLogRepository;
import com.petcare.backend.service.AdminReminderLogService;
import jakarta.persistence.criteria.Join;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminReminderLogServiceImpl implements AdminReminderLogService {
    private static final int MAX_PAGE_SIZE = 100;

    private final CareReminderLogRepository careReminderLogRepository;
    private final VaccinationReminderLogRepository vaccinationReminderLogRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminReminderLogResponse> getCustomReminderLogs(
            CareReminderLog.ReminderLogStatus status,
            CareReminder.ReminderCategory category,
            Long petId,
            Long userId,
            Instant from,
            Instant to,
            int page,
            int size
    ) {
        validateTimeRange(from, to);
        Pageable pageable = PageRequest.of(
                validatePage(page),
                validateSize(size),
                Sort.by(Sort.Direction.DESC, "dueAt")
        );

        return PageResponse.from(careReminderLogRepository
                .findAll(customLogSpecification(status, category, petId, userId, from, to), pageable)
                .map(AdminReminderLogResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminReminderLogResponse getCustomReminderLogDetail(Long logId) {
        return AdminReminderLogResponse.from(careReminderLogRepository.findById(logId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy log lịch nhắc")));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminVaccinationReminderLogResponse> getVaccinationReminderLogs(
            VaccinationReminderLog.VaccinationReminderStatus status,
            VaccinationReminderLog.VaccinationReminderStage stage,
            Long vaccinationId,
            Long petId,
            Long userId,
            Instant from,
            Instant to,
            int page,
            int size
    ) {
        validateTimeRange(from, to);
        Pageable pageable = PageRequest.of(
                validatePage(page),
                validateSize(size),
                Sort.by(Sort.Direction.DESC, "scheduledAt")
        );

        return PageResponse.from(vaccinationReminderLogRepository
                .findAll(vaccinationLogSpecification(status, stage, vaccinationId, petId, userId, from, to), pageable)
                .map(AdminVaccinationReminderLogResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminVaccinationReminderLogResponse getVaccinationReminderLogDetail(Long logId) {
        return AdminVaccinationReminderLogResponse.from(vaccinationReminderLogRepository.findById(logId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy log nhắc tiêm")));
    }

    private Specification<CareReminderLog> customLogSpecification(
            CareReminderLog.ReminderLogStatus status,
            CareReminder.ReminderCategory category,
            Long petId,
            Long userId,
            Instant from,
            Instant to
    ) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            Join<CareReminderLog, CareReminder> reminder = root.join("reminder");

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (category != null) {
                predicates.add(cb.equal(reminder.get("category"), category));
            }
            if (petId != null) {
                predicates.add(cb.equal(reminder.get("pet").get("id"), petId));
            }
            if (userId != null) {
                predicates.add(cb.equal(reminder.get("createdBy").get("id"), userId));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dueAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dueAt"), to));
            }

            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private Specification<VaccinationReminderLog> vaccinationLogSpecification(
            VaccinationReminderLog.VaccinationReminderStatus status,
            VaccinationReminderLog.VaccinationReminderStage stage,
            Long vaccinationId,
            Long petId,
            Long userId,
            Instant from,
            Instant to
    ) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (stage != null) {
                predicates.add(cb.equal(root.get("stage"), stage));
            }
            if (vaccinationId != null) {
                predicates.add(cb.equal(root.get("vaccination").get("id"), vaccinationId));
            }
            if (petId != null) {
                predicates.add(cb.equal(root.get("vaccination").get("pet").get("id"), petId));
            }
            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("scheduledAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("scheduledAt"), to));
            }

            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private void validateTimeRange(Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("Thời gian bắt đầu không được sau thời gian kết thúc");
        }
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
}
