package com.petcare.backend.controller;

import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.dto.locket.request.ReactPetMomentRequest;
import com.petcare.backend.dto.locket.response.PetMomentReactionDto;
import com.petcare.backend.dto.locket.response.PetMomentResponse;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.PetMomentService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/moments")
@RequiredArgsConstructor
public class PetMomentController {

    private final PetMomentService momentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PetMomentResponse>> createMoment(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("petId") Long petId,
            @RequestParam(value = "caption", required = false) String caption,
            @RequestParam(value = "locationName", required = false) String locationName,
            @RequestParam(value = "moodTag", required = false) String moodTag,
            @RequestParam(value = "audience", required = false, defaultValue = "FRIENDS") String audience,
            @RequestParam("file") MultipartFile file
    ) {
        PetMomentResponse response = momentService.createMoment(
                principal.getId(),
                petId,
                caption,
                locationName,
                moodTag,
                audience,
                file
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Đăng khoảnh khắc thành công", response));
    }

    @GetMapping("/feed")
    public ResponseEntity<ApiResponse<List<PetMomentResponse>>> getFeedMoments(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<PetMomentResponse> response = momentService.getFeedMoments(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách khoảnh khắc thành công", response));
    }

    @GetMapping("/pet/{petId}")
    public ResponseEntity<ApiResponse<List<PetMomentResponse>>> getPetMomentsHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long petId
    ) {
        List<PetMomentResponse> response = momentService.getPetMomentsHistory(principal.getId(), petId);
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử khoảnh khắc của thú cưng thành công", response));
    }

    @PostMapping("/{momentId}/react")
    public ResponseEntity<ApiResponse<PetMomentReactionDto>> reactToMoment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long momentId,
            @Valid @RequestBody ReactPetMomentRequest request
    ) {
        PetMomentReactionDto response = momentService.reactToMoment(principal.getId(), momentId, request.getEmoji());
        return ResponseEntity.ok(ApiResponse.success("Thả biểu cảm thành công", response));
    }

    @DeleteMapping("/{momentId}")
    public ResponseEntity<ApiResponse<Void>> deleteMoment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long momentId
    ) {
        momentService.deleteMoment(principal.getId(), momentId);
        return ResponseEntity.ok(ApiResponse.success("Xóa khoảnh khắc thành công", null));
    }
}
