package com.petcare.backend.service.impl;

import com.petcare.backend.dto.admin.vaccine.request.AdminCreateVaccineTemplateRequest;
import com.petcare.backend.dto.admin.vaccine.request.AdminUpdateVaccineTemplateRequest;
import com.petcare.backend.dto.admin.vaccine.response.AdminVaccineTemplateResponse;
import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.exception.ResourceNotFoundException;
import com.petcare.backend.model.Species;
import com.petcare.backend.model.VaccineTemplate;
import com.petcare.backend.repository.SpeciesRepository;
import com.petcare.backend.repository.VaccineTemplateRepository;
import com.petcare.backend.service.AdminVaccineTemplateService;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminVaccineTemplateServiceImpl implements AdminVaccineTemplateService {
    private static final int MAX_PAGE_SIZE = 100;

    private final VaccineTemplateRepository vaccineTemplateRepository;
    private final SpeciesRepository speciesRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminVaccineTemplateResponse> getTemplates(
            Long speciesId,
            String keyword,
            String seriesCode,
            VaccineTemplate.TargetStage targetStage,
            Boolean active,
            int page,
            int size
    ) {
        if (speciesId != null && !speciesRepository.existsById(speciesId)) {
            throw new BadRequestException("Loài không tồn tại");
        }

        return PageResponse.from(vaccineTemplateRepository
                .findAll(templateSpecification(speciesId, keyword, seriesCode, targetStage, active), pageable(page, size))
                .map(AdminVaccineTemplateResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminVaccineTemplateResponse getTemplateDetail(Long templateId) {
        return AdminVaccineTemplateResponse.from(getTemplateOrThrow(templateId));
    }

    @Override
    @Transactional
    public AdminVaccineTemplateResponse createTemplate(AdminCreateVaccineTemplateRequest request) {
        Species species = getSpeciesOrThrow(request.getSpeciesId());
        String vaccineName = normalizeRequired(request.getVaccineName(), "Tên vaccine không được để trống");
        String seriesCode = normalizeSeriesCode(request.getSeriesCode());
        Integer minimumAgeWeeks = request.getMinimumAgeWeeks();
        Integer intervalDays = request.getIntervalFromPreviousDays() == null ? 0 : request.getIntervalFromPreviousDays();
        Boolean optional = request.getOptional() == null ? false : request.getOptional();
        Boolean active = request.getActive() == null ? true : request.getActive();

        validateTemplateRule(
                species.getId(),
                seriesCode,
                request.getTargetStage(),
                request.getDoseNumber(),
                request.getBoosterIntervalMonths(),
                null
        );

        VaccineTemplate template = new VaccineTemplate();
        template.setSpecies(species);
        template.setVaccineName(vaccineName);
        template.setSeriesCode(seriesCode);
        template.setTargetStage(request.getTargetStage());
        template.setDoseNumber(request.getDoseNumber());
        template.setRecommendedAgeWeeks(minimumAgeWeeks);
        template.setMinimumAgeWeeks(minimumAgeWeeks);
        template.setIntervalFromPreviousDays(intervalDays);
        template.setBoosterIntervalMonths(request.getBoosterIntervalMonths());
        template.setOptional(optional);
        template.setActive(active);
        template.setDescription(trimToNull(request.getDescription()));
        return AdminVaccineTemplateResponse.from(vaccineTemplateRepository.save(template));
    }

    @Override
    @Transactional
    public AdminVaccineTemplateResponse updateTemplate(Long templateId, AdminUpdateVaccineTemplateRequest request) {
        VaccineTemplate template = getTemplateOrThrow(templateId);

        Species species = template.getSpecies();
        if (request.getSpeciesId() != null && !request.getSpeciesId().equals(species.getId())) {
            species = getSpeciesOrThrow(request.getSpeciesId());
        }

        String vaccineName = request.getVaccineName() == null
                ? template.getVaccineName()
                : normalizeRequired(request.getVaccineName(), "Tên vaccine không được để trống");
        String seriesCode = request.getSeriesCode() == null
                ? template.getSeriesCode()
                : normalizeSeriesCode(request.getSeriesCode());
        VaccineTemplate.TargetStage targetStage = request.getTargetStage() == null
                ? template.getTargetStage()
                : request.getTargetStage();
        Integer doseNumber = request.getDoseNumber() == null ? template.getDoseNumber() : request.getDoseNumber();
        Integer minimumAgeWeeks = request.getMinimumAgeWeeks() == null
                ? template.effectiveMinimumAgeWeeks()
                : request.getMinimumAgeWeeks();
        Integer intervalDays = request.getIntervalFromPreviousDays() == null
                ? template.getIntervalFromPreviousDays()
                : request.getIntervalFromPreviousDays();
        Integer boosterMonths = request.getBoosterIntervalMonths() == null
                ? template.getBoosterIntervalMonths()
                : request.getBoosterIntervalMonths();

        validateTemplateRule(species.getId(), seriesCode, targetStage, doseNumber, boosterMonths, templateId);

        template.setSpecies(species);
        template.setVaccineName(vaccineName);
        template.setSeriesCode(seriesCode);
        template.setTargetStage(targetStage);
        template.setDoseNumber(doseNumber);
        template.setRecommendedAgeWeeks(minimumAgeWeeks);
        template.setMinimumAgeWeeks(minimumAgeWeeks);
        template.setIntervalFromPreviousDays(intervalDays);
        template.setBoosterIntervalMonths(boosterMonths);

        if (request.getOptional() != null) {
            template.setOptional(request.getOptional());
        }
        if (request.getActive() != null) {
            template.setActive(request.getActive());
        }
        if (request.getDescription() != null) {
            template.setDescription(trimToNull(request.getDescription()));
        }

        return AdminVaccineTemplateResponse.from(vaccineTemplateRepository.save(template));
    }

    private Specification<VaccineTemplate> templateSpecification(
            Long speciesId,
            String keyword,
            String seriesCode,
            VaccineTemplate.TargetStage targetStage,
            Boolean active
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (speciesId != null) {
                predicates.add(cb.equal(root.get("species").get("id"), speciesId));
            }
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("vaccineName")), pattern),
                        cb.like(cb.lower(root.get("seriesCode")), pattern)
                ));
            }
            if (StringUtils.hasText(seriesCode)) {
                predicates.add(cb.equal(root.get("seriesCode"), normalizeSeriesCode(seriesCode)));
            }
            if (targetStage != null) {
                predicates.add(cb.equal(root.get("targetStage"), targetStage));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void validateTemplateRule(
            Long speciesId,
            String seriesCode,
            VaccineTemplate.TargetStage targetStage,
            Integer doseNumber,
            Integer boosterIntervalMonths,
            Long currentTemplateId
    ) {
        if (targetStage == null) {
            throw new BadRequestException("Giai đoạn tiêm không hợp lệ");
        }
        if (doseNumber == null || doseNumber < 1) {
            throw new BadRequestException("Số mũi phải lớn hơn hoặc bằng 1");
        }
        if (targetStage == VaccineTemplate.TargetStage.ADULT && boosterIntervalMonths == null) {
            throw new BadRequestException("Template ADULT cần có chu kỳ nhắc lại");
        }

        boolean duplicated = currentTemplateId == null
                ? vaccineTemplateRepository.existsBySpeciesIdAndSeriesCodeAndTargetStageAndDoseNumber(
                        speciesId,
                        seriesCode,
                        targetStage,
                        doseNumber
                )
                : vaccineTemplateRepository.existsBySpeciesIdAndSeriesCodeAndTargetStageAndDoseNumberAndIdNot(
                        speciesId,
                        seriesCode,
                        targetStage,
                        doseNumber,
                        currentTemplateId
                );
        if (duplicated) {
            throw new BadRequestException("Template vaccine đã tồn tại cho loài, series, giai đoạn và số mũi này");
        }
    }

    private Pageable pageable(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("Số trang không được âm");
        }
        if (size <= 0) {
            throw new BadRequestException("Kích thước trang phải lớn hơn 0");
        }
        return PageRequest.of(
                page,
                Math.min(size, MAX_PAGE_SIZE),
                Sort.by("species.id").ascending()
                        .and(Sort.by("seriesCode").ascending())
                        .and(Sort.by("targetStage").ascending())
                        .and(Sort.by("doseNumber").ascending())
        );
    }

    private VaccineTemplate getTemplateOrThrow(Long templateId) {
        return vaccineTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy template vaccine"));
    }

    private Species getSpeciesOrThrow(Long speciesId) {
        return speciesRepository.findById(speciesId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loài"));
    }

    private String normalizeSeriesCode(String value) {
        return normalizeRequired(value, "Mã series không được để trống")
                .toUpperCase(Locale.ROOT)
                .replace(" ", "_");
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException(message);
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
