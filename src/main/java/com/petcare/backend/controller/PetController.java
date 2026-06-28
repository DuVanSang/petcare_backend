package com.petcare.backend.controller;

import com.petcare.backend.dto.common.ApiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.dto.pet.request.CreateCoParentInvitationRequest;
import com.petcare.backend.dto.pet.request.CreatePetRequest;
import com.petcare.backend.dto.pet.request.UpdatePetRequest;
import com.petcare.backend.dto.pet.response.CoParentResponse;
import com.petcare.backend.dto.pet.response.CoParentInvitationResponse;
import com.petcare.backend.dto.pet.response.PetResponse;
import com.petcare.backend.dto.pet.response.PetSummaryResponse;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.PetService;
import com.petcare.backend.exception.BadRequestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/pets")
@RequiredArgsConstructor
@Tag(name = "Pets", description = "Quản lý thú cưng")
@SecurityRequirement(name = "bearerAuth")
public class PetController {

    private final PetService petService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Tạo thú cưng mới")
    public ResponseEntity<ApiResponse<PetResponse>> createPet(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreatePetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo thú cưng thành công", petService.createPet(principal, request)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PetResponse>> createPetMultipart(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestPart("data") String data,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar) {
        CreatePetRequest request = parseAndValidate(data, CreatePetRequest.class);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Pet created successfully",
                petService.createPet(principal.getId(), request, avatar)));
    }

    @GetMapping("/me")
    @Operation(summary = "Lấy danh sách thú cưng của tôi (owner + co-parent)")
    public ResponseEntity<ApiResponse<List<PetSummaryResponse>>> getMyPets(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách thú cưng thành công",
                petService.getMyPets(principal)
        ));
    }

    @GetMapping("/{petId}")
    @Operation(summary = "Xem chi tiết thú cưng")
    public ResponseEntity<ApiResponse<PetResponse>> getPet(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long petId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy thông tin thú cưng thành công",
                petService.getPetById(principal, petId)
        ));
    }

    @PatchMapping(value = "/{petId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cập nhật thông tin thú cưng")
    public ResponseEntity<ApiResponse<PetResponse>> updatePet(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long petId,
            @Valid @RequestBody UpdatePetRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật thú cưng thành công",
                petService.updatePet(principal, petId, request)
        ));
    }

    @PatchMapping(value = "/{petId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PetResponse>> updatePetMultipart(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long petId,
            @RequestPart("data") String data,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar) {
        UpdatePetRequest request = parseAndValidate(data, UpdatePetRequest.class);
        return ResponseEntity.ok(ApiResponse.success("Pet updated successfully",
                petService.updatePet(principal.getId(), petId, request, avatar)));
    }

    @DeleteMapping("/{petId}")
    @Operation(summary = "Xóa thú cưng (chỉ owner)")
    public ResponseEntity<ApiResponse<Void>> deletePet(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long petId) {
        petService.deletePet(principal, petId);
        return ResponseEntity.ok(ApiResponse.success("Xóa thú cưng thành công", null));
    }

    @GetMapping("/{petId}/co-parents")
    @Operation(summary = "Lấy danh sách đồng nuôi")
    public ResponseEntity<ApiResponse<List<CoParentResponse>>> getCoParents(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long petId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách đồng nuôi thành công",
                petService.getCoParents(principal, petId)
        ));
    }

    @PostMapping("/{petId}/co-parent-invitations")
    public ResponseEntity<ApiResponse<CoParentInvitationResponse>> createInvitation(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long petId,
            @Valid @RequestBody CreateCoParentInvitationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Invitation created successfully",
                petService.createCoParentInvitation(principal.getId(), petId, request)));
    }

    @GetMapping("/co-parent-invitations/incoming")
    public ResponseEntity<ApiResponse<PageResponse<CoParentInvitationResponse>>> incoming(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success("Incoming invitations retrieved successfully",
                petService.getIncomingInvitations(principal.getId(), page, size)));
    }

    @GetMapping("/co-parent-invitations/outgoing")
    public ResponseEntity<ApiResponse<PageResponse<CoParentInvitationResponse>>> outgoing(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success("Outgoing invitations retrieved successfully",
                petService.getOutgoingInvitations(principal.getId(), page, size)));
    }

    @GetMapping("/co-parent-invitations/{invitationId}")
    public ResponseEntity<ApiResponse<CoParentInvitationResponse>> invitation(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long invitationId) {
        return ResponseEntity.ok(ApiResponse.success("Invitation retrieved successfully",
                petService.getInvitation(principal.getId(), invitationId)));
    }

    @PatchMapping("/co-parent-invitations/{invitationId}/accept")
    public ResponseEntity<ApiResponse<CoParentInvitationResponse>> accept(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long invitationId) {
        return ResponseEntity.ok(ApiResponse.success("Invitation accepted successfully",
                petService.acceptCoParentInvitation(principal.getId(), invitationId)));
    }

    @PatchMapping("/co-parent-invitations/{invitationId}/decline")
    public ResponseEntity<ApiResponse<CoParentInvitationResponse>> decline(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long invitationId) {
        return ResponseEntity.ok(ApiResponse.success("Invitation declined successfully",
                petService.declineCoParentInvitation(principal.getId(), invitationId)));
    }

    @PatchMapping("/co-parent-invitations/{invitationId}/revoke")
    public ResponseEntity<ApiResponse<CoParentInvitationResponse>> revoke(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long invitationId) {
        return ResponseEntity.ok(ApiResponse.success("Invitation revoked successfully",
                petService.revokeCoParentInvitation(principal.getId(), invitationId)));
    }

    private <T> T parseAndValidate(String data, Class<T> type) {
        try {
            T request = objectMapper.readValue(data, type);
            var violations = validator.validate(request);
            if (!violations.isEmpty()) throw new BadRequestException(violations.iterator().next().getMessage());
            return request;
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Invalid pet data");
        }
    }
}
