package com.petcare.backend.service;

import com.petcare.backend.dto.pet.request.AcceptInvitationRequest;
import com.petcare.backend.dto.pet.request.CreatePetRequest;
import com.petcare.backend.dto.pet.request.InviteCoParentRequest;
import com.petcare.backend.dto.pet.request.UpdatePetRequest;
import com.petcare.backend.dto.pet.response.CoParentResponse;
import com.petcare.backend.dto.pet.response.PetResponse;
import com.petcare.backend.dto.pet.response.PetSummaryResponse;
import com.petcare.backend.security.UserPrincipal;

import java.util.List;

public interface PetService {

    PetResponse createPet(UserPrincipal principal, CreatePetRequest request);

    List<PetSummaryResponse> getMyPets(UserPrincipal principal);

    PetResponse getPetById(UserPrincipal principal, Long petId);

    PetResponse updatePet(UserPrincipal principal, Long petId, UpdatePetRequest request);

    void deletePet(UserPrincipal principal, Long petId);

    List<CoParentResponse> getCoParents(UserPrincipal principal, Long petId);

    void inviteCoParent(UserPrincipal principal, Long petId, InviteCoParentRequest request);

    void acceptInvitation(UserPrincipal principal, AcceptInvitationRequest request);
}
