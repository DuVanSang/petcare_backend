package com.petcare.backend.service;

import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.dto.pet.request.CreateCoParentInvitationRequest;
import com.petcare.backend.dto.pet.request.CreatePetRequest;
import com.petcare.backend.dto.pet.request.UpdatePetRequest;
import com.petcare.backend.dto.pet.request.UpdateCoParentRoleRequest;
import com.petcare.backend.dto.pet.response.CoParentResponse;
import com.petcare.backend.dto.pet.response.CoParentInvitationResponse;
import com.petcare.backend.dto.pet.response.PetResponse;
import com.petcare.backend.dto.pet.response.PetSummaryResponse;
import com.petcare.backend.security.UserPrincipal;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface PetService {

    PetResponse createPet(UserPrincipal principal, CreatePetRequest request);
    PetResponse createPet(Long currentUserId, CreatePetRequest request, MultipartFile avatar);

    List<PetSummaryResponse> getMyPets(UserPrincipal principal);

    PetResponse getPetById(UserPrincipal principal, Long petId);

    PetResponse updatePet(UserPrincipal principal, Long petId, UpdatePetRequest request);
    PetResponse updatePet(Long currentUserId, Long petId, UpdatePetRequest request, MultipartFile avatar);

    PetResponse archivePet(UserPrincipal principal, Long petId);

    void deletePet(UserPrincipal principal, Long petId);

    List<CoParentResponse> getCoParents(UserPrincipal principal, Long petId);
    CoParentResponse updateCoParentRole(Long currentUserId, Long petId, Long coParentId,
                                        UpdateCoParentRoleRequest request);
    void removeCoParent(Long currentUserId, Long petId, Long coParentId);

    CoParentInvitationResponse createCoParentInvitation(Long currentUserId, Long petId,
                                                        CreateCoParentInvitationRequest request);
    PageResponse<CoParentInvitationResponse> getIncomingInvitations(Long currentUserId, int page, int size);
    PageResponse<CoParentInvitationResponse> getOutgoingInvitations(Long currentUserId, int page, int size);
    CoParentInvitationResponse getInvitation(Long currentUserId, Long invitationId);
    CoParentInvitationResponse acceptCoParentInvitation(Long currentUserId, Long invitationId);
    CoParentInvitationResponse declineCoParentInvitation(Long currentUserId, Long invitationId);
    CoParentInvitationResponse revokeCoParentInvitation(Long currentUserId, Long invitationId);
}
