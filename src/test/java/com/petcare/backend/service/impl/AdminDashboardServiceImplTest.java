package com.petcare.backend.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petcare.backend.dto.admin.dashboard.response.AdminDashboardOverviewResponse;
import com.petcare.backend.model.Pet;
import com.petcare.backend.model.PetVaccination;
import com.petcare.backend.repository.CareReminderRepository;
import com.petcare.backend.repository.BlogRepository;
import com.petcare.backend.repository.NotificationRepository;
import com.petcare.backend.repository.PetRepository;
import com.petcare.backend.repository.PetVaccinationRepository;
import com.petcare.backend.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceImplTest {
    @Mock private UserRepository userRepository;
    @Mock private PetRepository petRepository;
    @Mock private PetVaccinationRepository petVaccinationRepository;
    @Mock private CareReminderRepository careReminderRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private BlogRepository blogRepository;
    private AdminDashboardServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminDashboardServiceImpl(userRepository, petRepository, petVaccinationRepository,
                careReminderRepository, notificationRepository, blogRepository);
    }

    @Test
    void getOverviewReturnsZeroesWhenRepositoriesHaveNoData() {
        AdminDashboardOverviewResponse response = service.getOverview();

        assertEquals(0, response.getTotalUsers());
        assertEquals(0, response.getActiveUsers());
        assertEquals(0, response.getBannedUsers());
        assertEquals(0, response.getNewUsersToday());
        assertEquals(0, response.getTotalPets());
        assertEquals(0, response.getActivePets());
        assertEquals(0, response.getTotalVaccinations());
        assertEquals(0, response.getProposedVaccinations());
        assertEquals(0, response.getScheduledVaccinations());
        assertEquals(0, response.getOverdueVaccinations());
        assertEquals(0, response.getCompletedVaccinations());
        assertEquals(0, response.getActiveReminders());
        assertEquals(0, response.getNotificationsSentToday());
    }

    @Test
    void getOverviewMapsEveryRepositoryCountAndUsesTodayWindow() {
        when(userRepository.count()).thenReturn(101L);
        when(userRepository.countByStatusAndDeletedAtIsNull("active")).thenReturn(80L);
        when(userRepository.countByStatusAndDeletedAtIsNull("banned")).thenReturn(3L);
        when(userRepository.countByCreatedAtBetweenAndDeletedAtIsNull(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(5L);
        when(petRepository.count()).thenReturn(78L);
        when(petRepository.countByStatus(Pet.PetStatus.active)).thenReturn(70L);
        when(petVaccinationRepository.count()).thenReturn(250L);
        when(petVaccinationRepository.countByStatus(PetVaccination.VaccinationStatus.proposed)).thenReturn(10L);
        when(petVaccinationRepository.countByStatus(PetVaccination.VaccinationStatus.scheduled)).thenReturn(20L);
        when(petVaccinationRepository.countByStatus(PetVaccination.VaccinationStatus.overdue)).thenReturn(4L);
        when(petVaccinationRepository.countByStatus(PetVaccination.VaccinationStatus.completed)).thenReturn(216L);
        when(careReminderRepository.countByActiveTrue()).thenReturn(41L);
        when(notificationRepository.countByStatusAndSentAtBetween(eq("sent"), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(13L);

        AdminDashboardOverviewResponse response = service.getOverview();

        assertEquals(101, response.getTotalUsers());
        assertEquals(80, response.getActiveUsers());
        assertEquals(3, response.getBannedUsers());
        assertEquals(5, response.getNewUsersToday());
        assertEquals(78, response.getTotalPets());
        assertEquals(70, response.getActivePets());
        assertEquals(250, response.getTotalVaccinations());
        assertEquals(10, response.getProposedVaccinations());
        assertEquals(20, response.getScheduledVaccinations());
        assertEquals(4, response.getOverdueVaccinations());
        assertEquals(216, response.getCompletedVaccinations());
        assertEquals(41, response.getActiveReminders());
        assertEquals(13, response.getNotificationsSentToday());

        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrow = start.plusDays(1);
        verify(userRepository).countByCreatedAtBetweenAndDeletedAtIsNull(start, tomorrow);
        verify(notificationRepository).countByStatusAndSentAtBetween("sent", start, tomorrow);
    }
}
