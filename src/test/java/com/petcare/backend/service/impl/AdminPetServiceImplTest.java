package com.petcare.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.exception.ResourceNotFoundException;
import com.petcare.backend.model.Pet;
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
}
