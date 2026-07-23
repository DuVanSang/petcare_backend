package com.petcare.backend.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petcare.backend.dto.admin.vaccine.request.AdminCreateVaccineTemplateRequest;
import com.petcare.backend.dto.admin.vaccine.request.AdminUpdateVaccineTemplateRequest;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.exception.ResourceNotFoundException;
import com.petcare.backend.model.Species;
import com.petcare.backend.model.VaccineTemplate;
import com.petcare.backend.repository.SpeciesRepository;
import com.petcare.backend.repository.VaccineTemplateRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
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
class AdminVaccineTemplateServiceImplTest {
    @Mock private VaccineTemplateRepository templates;
    @Mock private SpeciesRepository speciesRepository;
    private AdminVaccineTemplateServiceImpl service;

    @BeforeEach void setUp() { service = new AdminVaccineTemplateServiceImpl(templates, speciesRepository); }

    @Test
    void getTemplatesMapsFilteredDataCapsSizeAndExecutesSpecification() {
        Species dog = species(1L, "Dog"); when(speciesRepository.existsById(1L)).thenReturn(true); when(templates.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(template(2L, dog))));
        var result = service.getTemplates(1L, " rab ", " core series ", VaccineTemplate.TargetStage.PUPPY, true, 0, 101);
        assertEquals(1, result.getContent().size()); assertEquals("CORE_SERIES", result.getContent().getFirst().getSeriesCode());
        ArgumentCaptor<Specification> spec = ArgumentCaptor.forClass(Specification.class); ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class); verify(templates).findAll(spec.capture(), pageable.capture()); assertEquals(100, pageable.getValue().getPageSize()); execute(spec.getValue());
    }

    @Test
    void getTemplatesSupportsEmptyNoFilterAndRejectsUnknownSpeciesAndInvalidPaging() {
        when(templates.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        assertEquals(0, service.getTemplates(null, " ", null, null, null, 0, 1).getContent().size());
        ArgumentCaptor<Specification> spec = ArgumentCaptor.forClass(Specification.class); verify(templates).findAll(spec.capture(), any(Pageable.class)); execute(spec.getValue());
        when(speciesRepository.existsById(9L)).thenReturn(false); assertThrows(BadRequestException.class, () -> service.getTemplates(9L, null, null, null, null, 0, 1));
        assertThrows(BadRequestException.class, () -> service.getTemplates(null, null, null, null, null, -1, 1)); assertThrows(BadRequestException.class, () -> service.getTemplates(null, null, null, null, null, 0, 0));
    }

    @Test
    void detailMapsTemplateAndThrowsWhenMissing() {
        Species dog = species(1L, "Dog"); when(templates.findById(2L)).thenReturn(Optional.of(template(2L, dog)));
        var response = service.getTemplateDetail(2L); assertEquals(2L, response.getId()); assertEquals("Dog", response.getSpeciesName()); assertEquals("CORE_SERIES", response.getSeriesCode());
        when(templates.findById(9L)).thenReturn(Optional.empty()); assertThrows(ResourceNotFoundException.class, () -> service.getTemplateDetail(9L));
    }

    @Test
    void createMapsDefaultsNormalizesFieldsAndSaves() {
        Species dog = species(1L, "Dog"); when(speciesRepository.findById(1L)).thenReturn(Optional.of(dog)); when(templates.save(any(VaccineTemplate.class))).thenAnswer(i -> { VaccineTemplate saved = i.getArgument(0); saved.setId(3L); return saved; });
        AdminCreateVaccineTemplateRequest request = createRequest(); request.setIntervalFromPreviousDays(null); request.setOptional(null); request.setActive(null); request.setDescription(" ");
        var response = service.createTemplate(request);
        assertEquals(3L, response.getId()); assertEquals("Rabies", response.getVaccineName()); assertEquals("CORE_SERIES", response.getSeriesCode()); assertEquals(0, response.getIntervalFromPreviousDays()); assertFalse(response.getOptional()); assertEquals(true, response.getActive()); assertNull(response.getDescription());
    }

    @Test
    void createRejectsMissingSpeciesBlankFieldsDuplicateAndInvalidRuleBoundaries() {
        when(speciesRepository.findById(1L)).thenReturn(Optional.empty()); assertThrows(ResourceNotFoundException.class, () -> service.createTemplate(createRequest()));
        Species dog = species(1L, "Dog"); when(speciesRepository.findById(1L)).thenReturn(Optional.of(dog)); AdminCreateVaccineTemplateRequest request = createRequest(); request.setVaccineName(" "); assertThrows(BadRequestException.class, () -> service.createTemplate(request)); request.setVaccineName("Rabies"); request.setSeriesCode(" "); assertThrows(BadRequestException.class, () -> service.createTemplate(request)); request.setSeriesCode("core series"); request.setTargetStage(null); assertThrows(BadRequestException.class, () -> service.createTemplate(request)); request.setTargetStage(VaccineTemplate.TargetStage.PUPPY); request.setDoseNumber(0); assertThrows(BadRequestException.class, () -> service.createTemplate(request)); request.setDoseNumber(-1); assertThrows(BadRequestException.class, () -> service.createTemplate(request)); request.setDoseNumber(1); request.setTargetStage(VaccineTemplate.TargetStage.ADULT); request.setBoosterIntervalMonths(null); assertThrows(BadRequestException.class, () -> service.createTemplate(request)); request.setBoosterIntervalMonths(12); when(templates.existsBySpeciesIdAndSeriesCodeAndTargetStageAndDoseNumber(1L, "CORE_SERIES", VaccineTemplate.TargetStage.ADULT, 1)).thenReturn(true); assertThrows(BadRequestException.class, () -> service.createTemplate(request));
    }

    @Test
    void updateChangesSpeciesRuleAndOptionalFields() {
        Species dog = species(1L, "Dog"), cat = species(2L, "Cat"); VaccineTemplate existing = template(4L, dog); existing.setMinimumAgeWeeks(null); existing.setRecommendedAgeWeeks(8);
        when(templates.findById(4L)).thenReturn(Optional.of(existing)); when(speciesRepository.findById(2L)).thenReturn(Optional.of(cat)); when(templates.save(existing)).thenReturn(existing);
        AdminUpdateVaccineTemplateRequest request = new AdminUpdateVaccineTemplateRequest(); request.setSpeciesId(2L); request.setVaccineName(" New Vaccine "); request.setSeriesCode("new series"); request.setTargetStage(VaccineTemplate.TargetStage.CATCH_UP); request.setDoseNumber(2); request.setMinimumAgeWeeks(0); request.setIntervalFromPreviousDays(0); request.setOptional(true); request.setActive(false); request.setDescription(" desc ");
        var response = service.updateTemplate(4L, request);
        assertEquals(2L, response.getSpeciesId()); assertEquals("New Vaccine", response.getVaccineName()); assertEquals("NEW_SERIES", response.getSeriesCode()); assertEquals(0, response.getMinimumAgeWeeks()); assertEquals(true, response.getOptional()); assertEquals(false, response.getActive()); assertEquals("desc", response.getDescription());
    }

    @Test
    void updateRejectsNotFoundDuplicateAndInvalidAdultRule() {
        when(templates.findById(9L)).thenReturn(Optional.empty()); assertThrows(ResourceNotFoundException.class, () -> service.updateTemplate(9L, new AdminUpdateVaccineTemplateRequest()));
        Species dog = species(1L, "Dog"); VaccineTemplate existing = template(4L, dog); when(templates.findById(4L)).thenReturn(Optional.of(existing)); AdminUpdateVaccineTemplateRequest duplicate = new AdminUpdateVaccineTemplateRequest(); duplicate.setDoseNumber(2); when(templates.existsBySpeciesIdAndSeriesCodeAndTargetStageAndDoseNumberAndIdNot(1L, "CORE_SERIES", VaccineTemplate.TargetStage.PUPPY, 2, 4L)).thenReturn(true); assertThrows(BadRequestException.class, () -> service.updateTemplate(4L, duplicate));
        AdminUpdateVaccineTemplateRequest adult = new AdminUpdateVaccineTemplateRequest(); adult.setTargetStage(VaccineTemplate.TargetStage.ADULT); adult.setBoosterIntervalMonths(null); existing.setBoosterIntervalMonths(null); assertThrows(BadRequestException.class, () -> service.updateTemplate(4L, adult)); adult.setSpeciesId(7L); when(speciesRepository.findById(7L)).thenReturn(Optional.empty()); assertThrows(ResourceNotFoundException.class, () -> service.updateTemplate(4L, adult));
    }

    private void execute(Specification specification) { specification.toPredicate(root(), mock(CriteriaQuery.class), criteriaBuilder()); }
    private Root root() { Root root = mock(Root.class); Path path = mock(Path.class); when(root.get(any(String.class))).thenReturn(path); when(path.get(any(String.class))).thenReturn(path); return root; }
    private CriteriaBuilder criteriaBuilder() { CriteriaBuilder cb = mock(CriteriaBuilder.class); Predicate predicate = mock(Predicate.class); when(cb.lower(any())).thenReturn(mock(jakarta.persistence.criteria.Expression.class)); when(cb.like(any(), any(String.class))).thenReturn(predicate); when(cb.or(any(Predicate[].class))).thenReturn(predicate); when(cb.equal(any(), any())).thenReturn(predicate); when(cb.and(any(Predicate[].class))).thenReturn(predicate); return cb; }
    private AdminCreateVaccineTemplateRequest createRequest() { AdminCreateVaccineTemplateRequest request = new AdminCreateVaccineTemplateRequest(); request.setSpeciesId(1L); request.setVaccineName(" Rabies "); request.setSeriesCode("core series"); request.setTargetStage(VaccineTemplate.TargetStage.PUPPY); request.setDoseNumber(1); request.setMinimumAgeWeeks(6); request.setBoosterIntervalMonths(12); return request; }
    private Species species(Long id, String name) { Species species = new Species(); species.setId(id); species.setName(name); return species; }
    private VaccineTemplate template(Long id, Species species) { VaccineTemplate template = new VaccineTemplate(); template.setId(id); template.setSpecies(species); template.setVaccineName("Rabies"); template.setSeriesCode("CORE_SERIES"); template.setTargetStage(VaccineTemplate.TargetStage.PUPPY); template.setDoseNumber(1); template.setRecommendedAgeWeeks(6); template.setMinimumAgeWeeks(6); template.setIntervalFromPreviousDays(0); template.setBoosterIntervalMonths(12); template.setOptional(false); template.setActive(true); return template; }
}
