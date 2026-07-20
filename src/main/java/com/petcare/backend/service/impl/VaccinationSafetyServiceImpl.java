package com.petcare.backend.service.impl;

import com.petcare.backend.dto.vaccination.response.VaccinationSafetyWarningResponse;
import com.petcare.backend.model.HealthCondition;
import com.petcare.backend.model.HealthLog;
import com.petcare.backend.model.Pet;
import com.petcare.backend.model.PetVaccination;
import com.petcare.backend.repository.HealthConditionRepository;
import com.petcare.backend.repository.HealthLogRepository;
import com.petcare.backend.service.VaccinationSafetyService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class VaccinationSafetyServiceImpl implements VaccinationSafetyService {
    private final HealthConditionRepository healthConditionRepository;
    private final HealthLogRepository healthLogRepository;

    @Override
    public VaccinationSafetyWarningResponse evaluate(PetVaccination vaccination) {
        Pet pet = vaccination.getPet();
        List<String> reasons = new ArrayList<>();
        boolean highRisk = false;

        List<HealthCondition> activeConditions = healthConditionRepository.findByPetIdAndIsActiveTrue(pet.getId());
        for (HealthCondition condition : activeConditions) {
            if (condition.getType() == HealthCondition.ConditionType.allergy) {
                reasons.add("Có tiền sử dị ứng: " + condition.getTitle());
                highRisk = true;
            } else if (condition.getType() == HealthCondition.ConditionType.chronic_disease) {
                reasons.add("Có bệnh nền: " + condition.getTitle());
            } else if (condition.getType() == HealthCondition.ConditionType.current_medication) {
                reasons.add("Đang dùng thuốc/điều trị: " + condition.getTitle());
            }

            if (containsHighRiskKeyword(condition.getTitle()) || containsHighRiskKeyword(condition.getDescription())) {
                highRisk = true;
            }
        }

        HealthLog latestLog = healthLogRepository.findFirstByPetIdOrderByLoggedDateDesc(pet.getId()).orElse(null);
        if (latestLog != null) {
            if (latestLog.getAppetite() == HealthLog.Appetite.poor) {
                reasons.add("Health log gần nhất ghi nhận bé ăn kém");
            }
            if (latestLog.getActivityLevel() == HealthLog.ActivityLevel.low) {
                reasons.add("Health log gần nhất ghi nhận mức vận động thấp");
            }
            if (StringUtils.hasText(latestLog.getAbnormalEvent())) {
                reasons.add("Health log gần nhất có sự kiện bất thường: " + latestLog.getAbnormalEvent().trim());
                highRisk = true;
            }
            if (StringUtils.hasText(latestLog.getTreatmentNotes())) {
                reasons.add("Health log gần nhất có ghi chú điều trị: " + latestLog.getTreatmentNotes().trim());
            }
        }

        if (reasons.isEmpty()) {
            return VaccinationSafetyWarningResponse.builder()
                    .warning(false)
                    .warningLevel(VaccinationSafetyWarningResponse.WarningLevel.none)
                    .title("Không phát hiện cảnh báo y tế")
                    .message("Hệ thống chưa ghi nhận dị ứng, bệnh nền hoặc health log bất thường. Chủ nuôi vẫn nên xác nhận tình trạng sức khỏe của thú cưng với bác sĩ thú y trước khi tiêm.")
                    .reasons(List.of())
                    .latestHealthLogDate(latestLog != null ? latestLog.getLoggedDate() : null)
                    .build();
        }

        VaccinationSafetyWarningResponse.WarningLevel level = highRisk
                ? VaccinationSafetyWarningResponse.WarningLevel.high
                : VaccinationSafetyWarningResponse.WarningLevel.caution;
        String petName = pet.getName();
        String vaccineName = vaccination.getVaccineName();
        String title = level == VaccinationSafetyWarningResponse.WarningLevel.high
                ? "Cảnh báo y tế trước khi tiêm"
                : "Lưu ý sức khỏe trước khi tiêm";
        String message = "Bé " + petName + " đến lịch tiêm " + vaccineName
                + ". Vui lòng thông báo các thông tin sức khỏe dưới đây với bác sĩ thú y trước khi tiêm.";

        return VaccinationSafetyWarningResponse.builder()
                .warning(true)
                .warningLevel(level)
                .title(title)
                .message(message)
                .reasons(reasons)
                .latestHealthLogDate(latestLog != null ? latestLog.getLoggedDate() : null)
                .build();
    }

    private boolean containsHighRiskKeyword(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.contains("sốc")
                || normalized.contains("phan ve")
                || normalized.contains("phản vệ")
                || normalized.contains("shock")
                || normalized.contains("sốt")
                || normalized.contains("fever")
                || normalized.contains("mang thai")
                || normalized.contains("pregnan")
                || normalized.contains("suy giảm miễn dịch")
                || normalized.contains("immun");
    }
}
