package com.petcare.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.exception.ForbiddenException;
import com.petcare.backend.exception.ResourceNotFoundException;
import com.petcare.backend.model.Pet;
import com.petcare.backend.model.User;
import com.petcare.backend.repository.PetCoParentRepository;
import com.petcare.backend.repository.PetRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PetTagPermissionServiceImplTest {
    @Mock PetRepository pets; @Mock PetCoParentRepository coParents;
    @Test void validateTaggablePet_CoversNullBoundariesStatusOwnerEditorAndForbidden() { var service=new PetTagPermissionServiceImpl(pets,coParents); assertThat(service.validateAndGetTaggablePet(1L,null)).isNull();assertThatThrownBy(()->service.validateAndGetTaggablePet(1L,0L)).isInstanceOf(BadRequestException.class);when(pets.findById(2L)).thenReturn(Optional.empty());assertThatThrownBy(()->service.validateAndGetTaggablePet(1L,2L)).isInstanceOf(ResourceNotFoundException.class);Pet pet=pet(2L,1L);pet.setStatus(Pet.PetStatus.archived);when(pets.findById(2L)).thenReturn(Optional.of(pet));assertThatThrownBy(()->service.validateAndGetTaggablePet(1L,2L)).isInstanceOf(BadRequestException.class);pet.setStatus(Pet.PetStatus.active);assertThat(service.validateAndGetTaggablePet(1L,2L)).isSameAs(pet);when(coParents.existsByPetIdAndUserIdAndRole(org.mockito.ArgumentMatchers.anyLong(),org.mockito.ArgumentMatchers.anyLong(),org.mockito.ArgumentMatchers.any())).thenReturn(true);assertThat(service.validateAndGetTaggablePet(3L,2L)).isSameAs(pet);when(coParents.existsByPetIdAndUserIdAndRole(org.mockito.ArgumentMatchers.anyLong(),org.mockito.ArgumentMatchers.anyLong(),org.mockito.ArgumentMatchers.any())).thenReturn(false);assertThatThrownBy(()->service.validateAndGetTaggablePet(3L,2L)).isInstanceOf(ForbiddenException.class); }
    @Test void canTagPet_NullAndOwnerPartitions() {var service=new PetTagPermissionServiceImpl(pets,coParents);assertThat(service.canTagPet(null,null)).isFalse();assertThat(service.canTagPet(1L,null)).isFalse();assertThat(service.canTagPet(1L,pet(2L,1L))).isTrue();}
    private Pet pet(long id,long ownerId){User u=new User();u.setId(ownerId);Pet p=new Pet();p.setId(id);p.setOwner(u);p.setStatus(Pet.PetStatus.active);return p;}
}
