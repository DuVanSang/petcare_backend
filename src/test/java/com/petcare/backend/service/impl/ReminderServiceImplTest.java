package com.petcare.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petcare.backend.dto.reminder.request.CreateReminderRequest;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.model.CareReminder;
import com.petcare.backend.model.Pet;
import com.petcare.backend.model.PetCoParent;
import com.petcare.backend.model.PetVaccination;
import com.petcare.backend.model.User;
import com.petcare.backend.repository.CareReminderLogRepository;
import com.petcare.backend.repository.CareReminderRepository;
import com.petcare.backend.repository.PetCoParentRepository;
import com.petcare.backend.repository.PetRepository;
import com.petcare.backend.repository.PetVaccinationRepository;
import com.petcare.backend.repository.UserRepository;
import com.petcare.backend.security.UserPrincipal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReminderServiceImplTest {
    @Mock
    private CareReminderRepository reminderRepository;
    @Mock
    private CareReminderLogRepository logRepository;
    @Mock
    private PetRepository petRepository;
    @Mock
    private PetCoParentRepository coParentRepository;
    @Mock
    private PetVaccinationRepository vaccinationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserPrincipal principal;

    private ReminderServiceImpl service;
    private Pet pet;

    @BeforeEach
    void setUp() {
        service = new ReminderServiceImpl(
                reminderRepository,
                logRepository,
                petRepository,
                coParentRepository,
                vaccinationRepository,
                userRepository,
                new ReminderScheduleCalculator()
        );

        User owner = new User();
        owner.setId(1L);
        pet = new Pet();
        pet.setId(10L);
        pet.setName("Milo");
        pet.setOwner(owner);
    }

    @Test
    void viewerCannotCreateCustomReminder() {
        User viewer = new User();
        viewer.setId(2L);
        PetCoParent relation = new PetCoParent();
        relation.setPet(pet);
        relation.setUser(viewer);
        relation.setRole(PetCoParent.CoParentRole.viewer);
        when(principal.getId()).thenReturn(2L);
        when(petRepository.findByIdAndAccessibleByUserId(10L, 2L)).thenReturn(Optional.of(pet));
        when(coParentRepository.findByPetIdAndUserId(10L, 2L)).thenReturn(Optional.of(relation));

        assertThatThrownBy(() -> service.createReminder(principal, bathingRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Bạn không có quyền tạo nhắc nhở cho thú cưng này");

        verify(reminderRepository, never()).save(org.mockito.ArgumentMatchers.any(CareReminder.class));
    }

    @Test
    void proposedVaccinationCannotHaveCustomReminder() {
        User owner = pet.getOwner();
        owner.setTimezone("Asia/Ho_Chi_Minh");
        PetVaccination vaccination = new PetVaccination();
        vaccination.setId(20L);
        vaccination.setPet(pet);
        vaccination.setStatus(PetVaccination.VaccinationStatus.proposed);
        when(principal.getId()).thenReturn(1L);
        when(petRepository.findByIdAndAccessibleByUserId(10L, 1L)).thenReturn(Optional.of(pet));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(vaccinationRepository.findByIdAndPetId(20L, 10L)).thenReturn(Optional.of(vaccination));

        CreateReminderRequest request = bathingRequest();
        request.setCategory(CareReminder.ReminderCategory.vaccination);
        request.setVaccinationId(20L);
        request.setRepeat(CareReminder.ReminderFrequency.once);

        assertThatThrownBy(() -> service.createReminder(principal, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Chỉ có thể tạo nhắc nhở cho mũi tiêm đã lên lịch hoặc quá hạn");
    }

    private CreateReminderRequest bathingRequest() {
        CreateReminderRequest request = new CreateReminderRequest();
        request.setPetId(10L);
        request.setCategory(CareReminder.ReminderCategory.bathing);
        request.setDate(LocalDate.now().plusDays(1));
        request.setTime(LocalTime.of(9, 0));
        request.setRepeat(CareReminder.ReminderFrequency.weekly);
        return request;
    }
}
