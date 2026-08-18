package com.petcare.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petcare.backend.dto.health.request.CreateHealthLogRequest;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.model.*;
import com.petcare.backend.repository.*;
import com.petcare.backend.security.UserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HealthLogServiceImplTest {
    @Mock private PetRepository petRepository; @Mock private UserRepository userRepository; @Mock private PetCoParentRepository coParentRepository;
    @Mock private HealthLogRepository healthLogRepository; @Mock private WeightLogRepository weightLogRepository; @Mock private PetTimelineEventRepository timelineRepository;
    @Mock private UserPrincipal principal;
    private HealthLogServiceImpl service; private Pet pet; private User owner;

    @BeforeEach void setUp() {
        service=new HealthLogServiceImpl(petRepository,userRepository,coParentRepository,healthLogRepository,weightLogRepository,timelineRepository);
        owner=new User();owner.setId(1L);pet=new Pet();pet.setId(10L);pet.setName("Milo");pet.setOwner(owner);pet.setCurrentWeight(new BigDecimal("3.00"));
        when(principal.getId()).thenReturn(1L);when(petRepository.findByIdAndAccessibleByUserId(10L,1L)).thenReturn(Optional.of(pet));when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
    }

    // EP/BVA: owner creates a first log at valid lower weight boundary and notes are trimmed.
    @Test void createHealthLog_NewOwnerLog_SavesHealthWeightPetAndTimeline() {
        when(healthLogRepository.findByPetIdAndLoggedDate(10L,LocalDate.now())).thenReturn(Optional.empty());
        when(healthLogRepository.save(any(HealthLog.class))).thenAnswer(i->{HealthLog l=i.getArgument(0);l.setId(20L);return l;});
        when(weightLogRepository.save(any(WeightLog.class))).thenAnswer(i->{WeightLog l=i.getArgument(0);l.setId(30L);return l;});
        var result=service.createHealthLog(principal,request(new BigDecimal("0.01")," note "));
        ArgumentCaptor<HealthLog> log=ArgumentCaptor.forClass(HealthLog.class);verify(healthLogRepository).save(log.capture());
        assertThat(log.getValue().getTreatmentNotes()).isEqualTo("note");assertThat(pet.getCurrentWeight()).isEqualByComparingTo("0.01");assertThat(result.getWeight()).isEqualByComparingTo("0.01");
        verify(timelineRepository).save(any(PetTimelineEvent.class));
    }

    // EP: same pet/date updates the existing health log, while an empty note becomes null.
    @Test void createHealthLog_ExistingDailyLog_ReusesLog() {
        HealthLog existing=healthLog(LocalDate.now());when(healthLogRepository.findByPetIdAndLoggedDate(10L,LocalDate.now())).thenReturn(Optional.of(existing));
        when(healthLogRepository.save(existing)).thenReturn(existing);when(weightLogRepository.save(any())).thenAnswer(i->{WeightLog w=i.getArgument(0);w.setId(30L);return w;});
        service.createHealthLog(principal,request(new BigDecimal("4.20"),"   "));
        assertThat(existing.getTreatmentNotes()).isNull();verify(healthLogRepository).save(existing);
    }

    @Test void createHealthLog_ViewerAndMissingUser_AreRejected() {
        User viewer=new User();viewer.setId(2L);PetCoParent cp=new PetCoParent();cp.setRole(PetCoParent.CoParentRole.viewer);
        when(principal.getId()).thenReturn(2L);when(petRepository.findByIdAndAccessibleByUserId(10L,2L)).thenReturn(Optional.of(pet));when(coParentRepository.findByPetIdAndUserId(10L,2L)).thenReturn(Optional.of(cp));
        assertThatThrownBy(()->service.createHealthLog(principal,request(new BigDecimal("4"),null))).isInstanceOf(BadRequestException.class);
        when(principal.getId()).thenReturn(1L);when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(()->service.createHealthLog(principal,request(new BigDecimal("4"),null))).isInstanceOf(BadRequestException.class);
    }

    @Test void createHealthLog_EditorCanWriteButInaccessiblePetIsRejected() {
        User editor=new User();editor.setId(2L);PetCoParent cp=new PetCoParent();cp.setRole(PetCoParent.CoParentRole.editor);
        when(principal.getId()).thenReturn(2L);when(petRepository.findByIdAndAccessibleByUserId(10L,2L)).thenReturn(Optional.of(pet));when(coParentRepository.findByPetIdAndUserId(10L,2L)).thenReturn(Optional.of(cp));when(userRepository.findById(2L)).thenReturn(Optional.of(editor));
        when(healthLogRepository.save(any())).thenAnswer(i->{HealthLog h=i.getArgument(0);h.setId(1L);return h;});when(weightLogRepository.save(any())).thenAnswer(i->{WeightLog w=i.getArgument(0);w.setId(1L);return w;});
        service.createHealthLog(principal,request(new BigDecimal("4"),null));
        when(petRepository.findByIdAndAccessibleByUserId(99L,2L)).thenReturn(Optional.empty());
        CreateHealthLogRequest bad=request(new BigDecimal("4"),null);bad.setPetId(99L);assertThatThrownBy(()->service.createHealthLog(principal,bad)).isInstanceOf(BadRequestException.class);
    }

    @Test void getHealthLogs_MapsLatestWeightByDateAndEmptyPartitions() {
        HealthLog log=healthLog(LocalDate.now());WeightLog first=weight(new BigDecimal("3.0"),LocalDate.now());WeightLog replacement=weight(new BigDecimal("3.5"),LocalDate.now());
        when(weightLogRepository.findByPetIdOrderByLoggedDateAsc(10L)).thenReturn(List.of(first,replacement));when(healthLogRepository.findByPetIdOrderByLoggedDateDesc(10L)).thenReturn(List.of(log));
        assertThat(service.getHealthLogs(principal,10L)).singleElement().satisfies(r->assertThat(r.getWeight()).isEqualByComparingTo("3.5"));
        when(weightLogRepository.findByPetIdOrderByLoggedDateAsc(10L)).thenReturn(List.of());when(healthLogRepository.findByPetIdOrderByLoggedDateDesc(10L)).thenReturn(List.of());assertThat(service.getHealthLogs(principal,10L)).isEmpty();
    }

    @Test void getWeightLogsAndTimeline_MapExistingAndEmptyResults() {
        WeightLog weight=weight(new BigDecimal("4"),LocalDate.now());PetTimelineEvent event=new PetTimelineEvent();event.setId(1L);event.setPet(pet);event.setEventType(PetTimelineEvent.EventType.weight_updated);event.setEventDate(LocalDate.now());
        when(weightLogRepository.findByPetIdOrderByLoggedDateAsc(10L)).thenReturn(List.of(weight));when(timelineRepository.findByPetIdOrderByEventDateDescCreatedAtDesc(10L)).thenReturn(List.of(event));
        assertThat(service.getWeightLogs(principal,10L)).hasSize(1);assertThat(service.getTimeline(principal,10L)).hasSize(1);
    }

    private CreateHealthLogRequest request(BigDecimal weight,String notes){CreateHealthLogRequest r=new CreateHealthLogRequest();r.setPetId(10L);r.setDate(LocalDate.now());r.setWeight(weight);r.setAppetite(HealthLog.Appetite.good);r.setActivityLevel(HealthLog.ActivityLevel.active);r.setNotes(notes);return r;}
    private HealthLog healthLog(LocalDate date){HealthLog l=new HealthLog();l.setId(20L);l.setPet(pet);l.setLoggedDate(date);l.setAppetite(HealthLog.Appetite.good);l.setActivityLevel(HealthLog.ActivityLevel.active);return l;}
    private WeightLog weight(BigDecimal value,LocalDate date){WeightLog w=new WeightLog();w.setId(1L);w.setPet(pet);w.setWeight(value);w.setLoggedDate(date);return w;}
}
