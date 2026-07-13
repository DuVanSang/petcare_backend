package com.petcare.backend.config;

import com.petcare.backend.model.Species;
import com.petcare.backend.model.VaccineTemplate;
import com.petcare.backend.repository.SpeciesRepository;
import com.petcare.backend.repository.VaccineTemplateRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class VaccineTemplateDataInitializer implements ApplicationRunner {
    private static final long CANINE_SPECIES_ID = 1L;
    private static final long FELINE_SPECIES_ID = 2L;

    private final SpeciesRepository speciesRepository;
    private final VaccineTemplateRepository templateRepository;

    @Value("${app.vaccine.seed-enabled:true}")
    private boolean seedEnabled;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seedEnabled) {
            return;
        }
        speciesRepository.findById(CANINE_SPECIES_ID)
                .ifPresent(species -> seedRules(species, canineRules()));
        speciesRepository.findById(FELINE_SPECIES_ID)
                .ifPresent(species -> seedRules(species, felineRules()));
    }

    private List<SeedRule> canineRules() {
        return List.of(
                rule("CANINE_CORE_DHPP", "DHPP core - mũi 1", VaccineTemplate.TargetStage.PUPPY,
                        1, 8, 0, null, false,
                        "Lịch đề xuất tham khảo; cần bác sĩ xác nhận và tuân theo nhãn vaccine."),
                rule("CANINE_CORE_DHPP", "DHPP core - mũi 2", VaccineTemplate.TargetStage.PUPPY,
                        2, 12, 28, null, false, "Khoảng cách tham khảo 4 tuần."),
                rule("CANINE_CORE_DHPP", "DHPP core - mũi cuối puppy", VaccineTemplate.TargetStage.PUPPY,
                        3, 16, 28, null, false, "Mũi puppy core cuối không sớm hơn 16 tuần tuổi."),
                rule("CANINE_CORE_DHPP", "DHPP core - mũi 26+ tuần", VaccineTemplate.TargetStage.PUPPY,
                        4, 26, 70, null, false, "Mũi theo dõi ở hoặc ngay sau 26 tuần tuổi."),
                rule("CANINE_CORE_DHPP", "DHPP core catch-up", VaccineTemplate.TargetStage.CATCH_UP,
                        1, 26, 0, null, false, "Một liều MLV core thường đủ cho chó trên 26 tuần."),
                rule("CANINE_CORE_DHPP", "DHPP core catch-up nguy cơ cao", VaccineTemplate.TargetStage.CATCH_UP,
                        2, 26, 28, null, true, "Liều tùy chọn khi bác sĩ đánh giá nguy cơ cao."),
                rule("CANINE_CORE_DHPP", "DHPP core nhắc lại", VaccineTemplate.TargetStage.ADULT,
                        1, 26, 0, 36, false, "Chu kỳ tham khảo 36 tháng cho core MLV sau đáp ứng ban đầu."),
                rule("CANINE_RABIES", "Vaccine dại puppy", VaccineTemplate.TargetStage.PUPPY,
                        1, 12, 0, null, false, "Thời điểm phải tuân theo nhãn sản phẩm và quy định địa phương."),
                rule("CANINE_RABIES", "Vaccine dại catch-up", VaccineTemplate.TargetStage.CATCH_UP,
                        1, 26, 0, null, false, "Lịch catch-up cần bác sĩ và quy định địa phương xác nhận."),
                rule("CANINE_RABIES", "Vaccine dại nhắc lại", VaccineTemplate.TargetStage.ADULT,
                        1, 26, 0, 12, false, "Chu kỳ chỉ là cấu hình mặc định; ưu tiên luật và nhãn sản phẩm.")
        );
    }

    private List<SeedRule> felineRules() {
        return List.of(
                rule("FELINE_CORE_FVRCP", "FVRCP core - mũi 1", VaccineTemplate.TargetStage.PUPPY,
                        1, 8, 0, null, false,
                        "Lịch kitten tham khảo; cần bác sĩ xác nhận và tuân theo nhãn vaccine."),
                rule("FELINE_CORE_FVRCP", "FVRCP core - mũi 2", VaccineTemplate.TargetStage.PUPPY,
                        2, 12, 28, null, false, "Khoảng cách tham khảo 4 tuần."),
                rule("FELINE_CORE_FVRCP", "FVRCP core - mũi cuối kitten", VaccineTemplate.TargetStage.PUPPY,
                        3, 16, 28, null, false, "Mũi core cuối không sớm hơn 16 tuần tuổi."),
                rule("FELINE_CORE_FVRCP", "FVRCP core - mũi 26+ tuần", VaccineTemplate.TargetStage.PUPPY,
                        4, 26, 70, null, false, "Mũi theo dõi ở hoặc ngay sau 26 tuần tuổi."),
                rule("FELINE_CORE_FVRCP", "FVRCP core catch-up - mũi 1",
                        VaccineTemplate.TargetStage.CATCH_UP,
                        1, 26, 0, null, false, "Mũi đầu của lịch catch-up cho mèo trưởng thành."),
                rule("FELINE_CORE_FVRCP", "FVRCP core catch-up - mũi 2",
                        VaccineTemplate.TargetStage.CATCH_UP,
                        2, 26, 28, null, false, "Mũi thứ hai cách mũi đầu 4 tuần."),
                rule("FELINE_CORE_FVRCP", "FVRCP core nhắc lại", VaccineTemplate.TargetStage.ADULT,
                        1, 26, 0, 36, false,
                        "Chu kỳ tham khảo 36 tháng cho mèo nguy cơ thấp sau phác đồ nền."),
                rule("FELINE_RABIES", "Vaccine dại kitten", VaccineTemplate.TargetStage.PUPPY,
                        1, 12, 0, null, false,
                        "Thời điểm phải tuân theo nhãn sản phẩm và quy định địa phương."),
                rule("FELINE_RABIES", "Vaccine dại catch-up", VaccineTemplate.TargetStage.CATCH_UP,
                        1, 26, 0, null, false,
                        "Lịch catch-up cần bác sĩ và quy định địa phương xác nhận."),
                rule("FELINE_RABIES", "Vaccine dại nhắc lại", VaccineTemplate.TargetStage.ADULT,
                        1, 26, 0, 12, false,
                        "Chu kỳ chỉ là cấu hình mặc định; ưu tiên luật và nhãn sản phẩm.")
        );
    }

    private void seedRules(Species species, List<SeedRule> rules) {
        rules.forEach(rule -> insertIfMissing(species, rule));
    }

    private void insertIfMissing(Species species, SeedRule rule) {
        if (templateRepository.existsBySpeciesIdAndSeriesCodeAndTargetStageAndDoseNumber(
                species.getId(), rule.seriesCode(), rule.stage(), rule.doseNumber())) {
            return;
        }
        VaccineTemplate template = new VaccineTemplate();
        template.setSpecies(species);
        template.setSeriesCode(rule.seriesCode());
        template.setVaccineName(rule.vaccineName());
        template.setTargetStage(rule.stage());
        template.setDoseNumber(rule.doseNumber());
        template.setRecommendedAgeWeeks(rule.minimumAgeWeeks());
        template.setMinimumAgeWeeks(rule.minimumAgeWeeks());
        template.setIntervalFromPreviousDays(rule.intervalDays());
        template.setBoosterIntervalMonths(rule.boosterMonths());
        template.setOptional(rule.optional());
        template.setActive(true);
        template.setDescription(rule.description());
        templateRepository.save(template);
    }

    private SeedRule rule(
            String seriesCode,
            String vaccineName,
            VaccineTemplate.TargetStage stage,
            int doseNumber,
            int minimumAgeWeeks,
            int intervalDays,
            Integer boosterMonths,
            boolean optional,
            String description) {
        return new SeedRule(seriesCode, vaccineName, stage, doseNumber, minimumAgeWeeks,
                intervalDays, boosterMonths, optional, description);
    }

    private record SeedRule(
            String seriesCode,
            String vaccineName,
            VaccineTemplate.TargetStage stage,
            int doseNumber,
            int minimumAgeWeeks,
            int intervalDays,
            Integer boosterMonths,
            boolean optional,
            String description) {
    }
}
