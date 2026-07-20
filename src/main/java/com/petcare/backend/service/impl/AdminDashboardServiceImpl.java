package com.petcare.backend.service.impl;

import com.petcare.backend.dto.admin.dashboard.response.AdminDashboardOverviewResponse;
import com.petcare.backend.model.Blog;
import com.petcare.backend.model.Pet;
import com.petcare.backend.model.PetVaccination;
import com.petcare.backend.repository.BlogRepository;
import com.petcare.backend.repository.CareReminderRepository;
import com.petcare.backend.repository.NotificationRepository;
import com.petcare.backend.repository.PetRepository;
import com.petcare.backend.repository.PetVaccinationRepository;
import com.petcare.backend.repository.UserRepository;
import com.petcare.backend.service.AdminDashboardService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {
    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final PetVaccinationRepository petVaccinationRepository;
    private final CareReminderRepository careReminderRepository;
    private final NotificationRepository notificationRepository;
    private final BlogRepository blogRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardOverviewResponse getOverview() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfTomorrow = startOfToday.plusDays(1);

        return AdminDashboardOverviewResponse.builder()
                .totalUsers(userRepository.count())
                .activeUsers(userRepository.countByStatusAndDeletedAtIsNull("active"))
                .bannedUsers(userRepository.countByStatusAndDeletedAtIsNull("banned"))
                .newUsersToday(userRepository.countByCreatedAtBetweenAndDeletedAtIsNull(startOfToday, startOfTomorrow))
                .totalPets(petRepository.count())
                .activePets(petRepository.countByStatus(Pet.PetStatus.active))
                .totalVaccinations(petVaccinationRepository.count())
                .proposedVaccinations(petVaccinationRepository.countByStatus(PetVaccination.VaccinationStatus.proposed))
                .scheduledVaccinations(petVaccinationRepository.countByStatus(PetVaccination.VaccinationStatus.scheduled))
                .overdueVaccinations(petVaccinationRepository.countByStatus(PetVaccination.VaccinationStatus.overdue))
                .completedVaccinations(petVaccinationRepository.countByStatus(PetVaccination.VaccinationStatus.completed))
                .activeReminders(careReminderRepository.countByActiveTrue())
                .totalBlogs(blogRepository.count())
                .publishedBlogs(blogRepository.countByStatus(Blog.BlogStatus.published))
                .totalNotifications(notificationRepository.count())
                .notificationsSentToday(notificationRepository.countByStatusAndSentAtBetween(
                        "sent",
                        startOfToday,
                        startOfTomorrow
                ))
                .build();
    }
}
