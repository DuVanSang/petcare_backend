package com.petcare.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.exception.ResourceNotFoundException;
import com.petcare.backend.model.Pet;
import com.petcare.backend.model.PetVaccination;
import com.petcare.backend.repository.PetCoParentRepository;
import com.petcare.backend.repository.PetRepository;
import com.petcare.backend.repository.PetVaccinationRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminPetServiceImplTest {
 @Mock PetRepository pets;@Mock PetCoParentRepository coParents;@Mock PetVaccinationRepository vaccinations;
 @Test void getPets_EmptyKeywordAndPageBoundaries_AreHandled(){var s=new AdminPetServiceImpl(pets,coParents,vaccinations);when(pets.searchForAdmin(any(),any(),any(),any(),any(),any())).thenReturn(new PageImpl<>(List.of()));assertThat(s.getPets(" ",null,null,null,null,0,1000).getContent()).isEmpty();assertThat(s.getPets(" milo ",null,null,null,null,0,1).getContent()).isEmpty();assertThatThrownBy(()->s.getPets(null,null,null,null,null,-1,1)).isInstanceOf(BadRequestException.class);assertThatThrownBy(()->s.getPets(null,null,null,null,null,0,0)).isInstanceOf(BadRequestException.class);}
 @Test void detailAndVaccinations_MissingAndBothStatusPartitions(){var s=new AdminPetServiceImpl(pets,coParents,vaccinations);when(pets.findById(1L)).thenReturn(Optional.empty());assertThatThrownBy(()->s.getPetDetail(1L)).isInstanceOf(ResourceNotFoundException.class);Pet p=new Pet();p.setId(1L);p.setName("Milo");when(pets.findById(1L)).thenReturn(Optional.of(p));when(coParents.findByPetId(1L)).thenReturn(List.of());when(vaccinations.findByPetIdOrderByScheduledDateAsc(1L)).thenReturn(List.of());when(vaccinations.findByPetIdAndStatusOrderByScheduledDateAsc(1L,com.petcare.backend.model.PetVaccination.VaccinationStatus.scheduled)).thenReturn(List.of());assertThat(s.getPetVaccinations(1L,null)).isEmpty();assertThat(s.getPetVaccinations(1L,com.petcare.backend.model.PetVaccination.VaccinationStatus.scheduled)).isEmpty();}
 @Test void getPets_MapsPopulatedPetAndVaccinationStatistics(){var s=new AdminPetServiceImpl(pets,coParents,vaccinations);Pet p=pet(9L,"Milo");when(pets.searchForAdmin(any(),any(),any(),any(),any(),any())).thenReturn(new PageImpl<>(List.of(p)));when(coParents.findByPetId(9L)).thenReturn(List.of());when(vaccinations.countByPetId(9L)).thenReturn(7L);when(vaccinations.countByPetIdAndStatus(9L,com.petcare.backend.model.PetVaccination.VaccinationStatus.scheduled)).thenReturn(3L);when(vaccinations.countByPetIdAndStatus(9L,com.petcare.backend.model.PetVaccination.VaccinationStatus.overdue)).thenReturn(1L);when(vaccinations.countByPetIdAndStatus(9L,com.petcare.backend.model.PetVaccination.VaccinationStatus.completed)).thenReturn(2L);var response=s.getPets("  Milo ",2L,3L,Pet.PetStatus.active,Pet.VaccinePlanStatus.ACTIVE,0,20);assertThat(response.getContent()).singleElement().satisfies(item->{assertThat(item.getId()).isEqualTo(9L);assertThat(item.getTotalVaccinations()).isEqualTo(7L);assertThat(item.getScheduledVaccinations()).isEqualTo(3L);assertThat(item.getOverdueVaccinations()).isEqualTo(1L);assertThat(item.getCompletedVaccinations()).isEqualTo(2L);});verify(pets).searchForAdmin(eq("Milo"),eq(2L),eq(3L),eq(Pet.PetStatus.active),eq(Pet.VaccinePlanStatus.ACTIVE),any());}
 @Test void getPetDetailAndVaccinations_MapExistingPetAndNonEmptyVaccinationList(){var s=new AdminPetServiceImpl(pets,coParents,vaccinations);Pet p=pet(1L,"Milo");when(pets.findById(1L)).thenReturn(Optional.of(p));when(coParents.findByPetId(1L)).thenReturn(List.of());when(vaccinations.countByPetId(1L)).thenReturn(9L);when(vaccinations.countByPetIdAndStatus(1L,com.petcare.backend.model.PetVaccination.VaccinationStatus.scheduled)).thenReturn(4L);when(vaccinations.countByPetIdAndStatus(1L,com.petcare.backend.model.PetVaccination.VaccinationStatus.overdue)).thenReturn(2L);when(vaccinations.countByPetIdAndStatus(1L,com.petcare.backend.model.PetVaccination.VaccinationStatus.completed)).thenReturn(3L);var detail=s.getPetDetail(1L);assertThat(detail.getId()).isEqualTo(1L);assertThat(detail.getTotalVaccinations()).isEqualTo(9L);PetVaccination vaccination=new PetVaccination();vaccination.setId(4L);vaccination.setPet(p);vaccination.setVaccineName("Rabies");vaccination.setStatus(com.petcare.backend.model.PetVaccination.VaccinationStatus.scheduled);when(vaccinations.findByPetIdOrderByScheduledDateAsc(1L)).thenReturn(List.of(vaccination));assertThat(s.getPetVaccinations(1L,null)).singleElement().satisfies(item->{assertThat(item.getId()).isEqualTo(4L);assertThat(item.getPetName()).isEqualTo("Milo");assertThat(item.getStatus()).isEqualTo("scheduled");});}
 private Pet pet(Long id,String name){Pet pet=new Pet();pet.setId(id);pet.setName(name);pet.setStatus(Pet.PetStatus.active);return pet;}
}
