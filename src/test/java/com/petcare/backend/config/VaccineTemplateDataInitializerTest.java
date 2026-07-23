package com.petcare.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.petcare.backend.model.Species;
import com.petcare.backend.model.VaccineTemplate;
import com.petcare.backend.repository.SpeciesRepository;
import com.petcare.backend.repository.VaccineTemplateRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class VaccineTemplateDataInitializerTest {
    @Mock private SpeciesRepository speciesRepository;
    @Mock private VaccineTemplateRepository templates;

    private VaccineTemplateDataInitializer initializer(boolean enabled) {
        VaccineTemplateDataInitializer initializer = new VaccineTemplateDataInitializer(speciesRepository, templates);
        ReflectionTestUtils.setField(initializer, "seedEnabled", enabled);
        return initializer;
    }

    @Test
    void run_DoesNothingWhenSeedIsDisabled() throws Exception {
        initializer(false).run(null);
        verifyNoInteractions(speciesRepository, templates);
    }

    @Test
    void run_SeedsAllCanineAndFelineRulesWhenSpeciesExist() throws Exception {
        Species canine = new Species(); canine.setId(1L);
        Species feline = new Species(); feline.setId(2L);
        when(speciesRepository.findById(1L)).thenReturn(Optional.of(canine));
        when(speciesRepository.findById(2L)).thenReturn(Optional.of(feline));
        when(templates.existsBySpeciesIdAndSeriesCodeAndTargetStageAndDoseNumber(anyLong(), any(), any(), anyInt())).thenReturn(false);

        initializer(true).run(null);

        ArgumentCaptor<VaccineTemplate> saved = ArgumentCaptor.forClass(VaccineTemplate.class);
        verify(templates, times(20)).save(saved.capture());
        assertThat(saved.getAllValues()).allSatisfy(template -> {
            assertThat(template.getActive()).isTrue();
            assertThat(template.getSpecies()).isIn(canine, feline);
            assertThat(template.getSeriesCode()).isNotBlank();
        });
    }

    @Test
    void run_SkipsExistingRulesAndMissingSpecies() throws Exception {
        Species canine = new Species(); canine.setId(1L);
        when(speciesRepository.findById(1L)).thenReturn(Optional.of(canine));
        when(speciesRepository.findById(2L)).thenReturn(Optional.empty());
        when(templates.existsBySpeciesIdAndSeriesCodeAndTargetStageAndDoseNumber(anyLong(), any(), any(), anyInt())).thenReturn(true);

        initializer(true).run(null);

        verify(templates, times(10)).existsBySpeciesIdAndSeriesCodeAndTargetStageAndDoseNumber(anyLong(), any(), any(), anyInt());
        verify(templates, never()).save(any());
    }
}
