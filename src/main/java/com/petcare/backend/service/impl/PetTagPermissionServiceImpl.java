package com.petcare.backend.service.impl;

import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.exception.ForbiddenException;
import com.petcare.backend.exception.ResourceNotFoundException;
import com.petcare.backend.model.Pet;
import com.petcare.backend.model.PetCoParent;
import com.petcare.backend.repository.PetCoParentRepository;
import com.petcare.backend.repository.PetRepository;
import com.petcare.backend.service.PetTagPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PetTagPermissionServiceImpl implements PetTagPermissionService {
    private final PetRepository petRepository;
    private final PetCoParentRepository petCoParentRepository;

    @Override
    public Pet validateAndGetTaggablePet(Long currentUserId, Long petId) {
        if (petId == null) {
            return null;
        }
        if (petId <= 0) {
            throw new BadRequestException("Pet id must be greater than 0");
        }

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));

        if (!Pet.PetStatus.active.equals(pet.getStatus())) {
            throw new BadRequestException("Cannot tag inactive pet");
        }

        if (!canTagPet(currentUserId, pet)) {
            throw new ForbiddenException("You do not have permission to tag this pet");
        }

        return pet;
    }

    @Override
    public boolean canTagPet(Long currentUserId, Pet pet) {
        if (currentUserId == null || pet == null) {
            return false;
        }
        if (pet.getOwner() != null && currentUserId.equals(pet.getOwner().getId())) {
            return true;
        }
        return petCoParentRepository.existsByPetIdAndUserIdAndRole(
                pet.getId(),
                currentUserId,
                PetCoParent.CoParentRole.editor
        );
    }
}
