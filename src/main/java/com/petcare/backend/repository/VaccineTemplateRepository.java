package com.petcare.backend.repository;

import com.petcare.backend.model.VaccineTemplate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VaccineTemplateRepository extends JpaRepository<VaccineTemplate, Long> {
    List<VaccineTemplate> findBySpeciesId(Long speciesId);

    List<VaccineTemplate> findBySpeciesIdAndActiveTrueAndSeriesCodeIsNotNullOrderBySeriesCodeAscDoseNumberAsc(
            Long speciesId
    );

    List<VaccineTemplate> findBySpeciesIdAndTargetStageAndActiveTrueOrderBySeriesCodeAscDoseNumberAsc(
            Long speciesId,
            VaccineTemplate.TargetStage targetStage
    );

    List<VaccineTemplate> findBySpeciesIdAndSeriesCodeAndTargetStageAndActiveTrueOrderByDoseNumberAsc(
            Long speciesId,
            String seriesCode,
            VaccineTemplate.TargetStage targetStage
    );

    Optional<VaccineTemplate> findFirstBySpeciesIdAndSeriesCodeAndTargetStageAndActiveTrue(
            Long speciesId,
            String seriesCode,
            VaccineTemplate.TargetStage targetStage
    );

    boolean existsBySpeciesIdAndSeriesCodeAndTargetStageAndDoseNumber(
            Long speciesId,
            String seriesCode,
            VaccineTemplate.TargetStage targetStage,
            Integer doseNumber
    );
}
