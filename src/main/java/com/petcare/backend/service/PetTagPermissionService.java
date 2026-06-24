package com.petcare.backend.service;

import com.petcare.backend.model.Pet;

public interface PetTagPermissionService {
    Pet validateAndGetTaggablePet(Long currentUserId, Long petId);

    boolean canTagPet(Long currentUserId, Pet pet);
}
