package com.petcare.backend.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.exception.ResourceNotFoundException;
import com.petcare.backend.model.CareReminder;
import com.petcare.backend.model.CareReminderLog;
import com.petcare.backend.model.VaccinationReminderLog;
import com.petcare.backend.repository.CareReminderLogRepository;
import com.petcare.backend.repository.VaccinationReminderLogRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
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
class AdminReminderLogServiceImplTest {
    @Mock private CareReminderLogRepository customLogs;
    @Mock private VaccinationReminderLogRepository vaccinationLogs;
    private AdminReminderLogServiceImpl service;

    @BeforeEach
    void setUp() { service = new AdminReminderLogServiceImpl(customLogs, vaccinationLogs); }

    @Test
    void getCustomReminderLogsAppliesEveryFilterCapsPageSizeAndMapsData() {
        when(customLogs.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(customLog(1L))));
        Instant from = Instant.parse("2026-07-01T00:00:00Z"), to = Instant.parse("2026-07-02T00:00:00Z");

        var response = service.getCustomReminderLogs(CareReminderLog.ReminderLogStatus.notified,
                CareReminder.ReminderCategory.medication, 10L, 20L, from, to, 0, 101);

        assertEquals(1, response.getContent().size()); assertEquals(1L, response.getContent().getFirst().getId());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        ArgumentCaptor<Specification> specification = ArgumentCaptor.forClass(Specification.class);
        verify(customLogs).findAll(specification.capture(), pageable.capture());
        assertEquals(0, pageable.getValue().getPageNumber()); assertEquals(100, pageable.getValue().getPageSize());
        assertEquals("dueAt: DESC", pageable.getValue().getSort().toString());
        executeCustomSpecification(specification.getValue());
    }

    @Test
    void getCustomReminderLogsSupportsNoFiltersAndEmptyPage() {
        when(customLogs.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        var response = service.getCustomReminderLogs(null, null, null, null, null, null, 0, 1);
        assertEquals(0, response.getContent().size());
        ArgumentCaptor<Specification> specification = ArgumentCaptor.forClass(Specification.class);
        verify(customLogs).findAll(specification.capture(), any(Pageable.class));
        executeCustomSpecification(specification.getValue());
    }

    @Test
    void getVaccinationReminderLogsAppliesEveryFilterAndMapsData() {
        when(vaccinationLogs.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(vaccinationLog(2L))));
        Instant from = Instant.parse("2026-07-01T00:00:00Z"), to = Instant.parse("2026-07-02T00:00:00Z");

        var response = service.getVaccinationReminderLogs(VaccinationReminderLog.VaccinationReminderStatus.notified,
                VaccinationReminderLog.VaccinationReminderStage.DUE_TODAY, 3L, 4L, 5L, from, to, 1, 20);

        assertEquals(1, response.getContent().size()); assertEquals(2L, response.getContent().getFirst().getId());
        ArgumentCaptor<Specification> specification = ArgumentCaptor.forClass(Specification.class);
        verify(vaccinationLogs).findAll(specification.capture(), any(Pageable.class));
        executeVaccinationSpecification(specification.getValue());
    }

    @Test
    void getVaccinationReminderLogsSupportsNoFiltersAndEmptyPage() {
        when(vaccinationLogs.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        assertEquals(0, service.getVaccinationReminderLogs(null, null, null, null, null, null, null, 0, 1)
                .getContent().size());
        ArgumentCaptor<Specification> specification = ArgumentCaptor.forClass(Specification.class);
        verify(vaccinationLogs).findAll(specification.capture(), any(Pageable.class));
        executeVaccinationSpecification(specification.getValue());
    }

    @Test
    void detailMethodsMapOptionalEntityAndThrowWhenAbsent() {
        when(customLogs.findById(1L)).thenReturn(Optional.of(customLog(1L)));
        when(vaccinationLogs.findById(2L)).thenReturn(Optional.of(vaccinationLog(2L)));
        assertEquals(1L, service.getCustomReminderLogDetail(1L).getId());
        assertNull(service.getCustomReminderLogDetail(1L).getReminderId());
        assertEquals(2L, service.getVaccinationReminderLogDetail(2L).getId());
        assertNull(service.getVaccinationReminderLogDetail(2L).getVaccinationId());
        when(customLogs.findById(9L)).thenReturn(Optional.empty());
        when(vaccinationLogs.findById(9L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getCustomReminderLogDetail(9L));
        assertThrows(ResourceNotFoundException.class, () -> service.getVaccinationReminderLogDetail(9L));
    }

    @Test
    void listValidationRejectsInvertedTimeRangeNegativePageAndNonPositiveSize() {
        Instant later = Instant.parse("2026-07-02T00:00:00Z"), earlier = Instant.parse("2026-07-01T00:00:00Z");
        assertThrows(BadRequestException.class, () -> service.getCustomReminderLogs(null, null, null, null, later, earlier, 0, 1));
        assertThrows(BadRequestException.class, () -> service.getCustomReminderLogs(null, null, null, null, null, null, -1, 1));
        assertThrows(BadRequestException.class, () -> service.getVaccinationReminderLogs(null, null, null, null, null, null, null, 0, 0));
    }

    private void executeCustomSpecification(Specification specification) { specification.toPredicate(customRoot(), mock(CriteriaQuery.class), criteriaBuilder()); }
    private void executeVaccinationSpecification(Specification specification) { specification.toPredicate(root(), mock(CriteriaQuery.class), criteriaBuilder()); }
    private Root<CareReminderLog> customRoot() { Root root = root(); Join join = mock(Join.class); Path path = path(); when(root.join("reminder")).thenReturn(join); when(join.get(any(String.class))).thenReturn(path); return root; }
    private Root root() { Root root = mock(Root.class); Path path = path(); when(root.get(any(String.class))).thenReturn(path); return root; }
    private Path path() { Path path = mock(Path.class); when(path.get(any(String.class))).thenReturn(path); return path; }
    private CriteriaBuilder criteriaBuilder() { CriteriaBuilder cb = mock(CriteriaBuilder.class); Predicate predicate = mock(Predicate.class); when(cb.equal(any(), any())).thenReturn(predicate); when(cb.greaterThanOrEqualTo(any(), any(Instant.class))).thenReturn(predicate); when(cb.lessThanOrEqualTo(any(), any(Instant.class))).thenReturn(predicate); when(cb.and(any(Predicate[].class))).thenReturn(predicate); return cb; }
    private CareReminderLog customLog(Long id) { CareReminderLog log = new CareReminderLog(); log.setId(id); return log; }
    private VaccinationReminderLog vaccinationLog(Long id) { VaccinationReminderLog log = new VaccinationReminderLog(); log.setId(id); return log; }
}
