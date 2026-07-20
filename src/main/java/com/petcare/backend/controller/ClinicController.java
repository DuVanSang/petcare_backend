package com.petcare.backend.controller;

import com.petcare.backend.dto.clinic.response.NearbyClinicResponse;
import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.service.ClinicSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clinics")
@RequiredArgsConstructor
@Tag(name = "Clinics", description = "Tìm phòng khám thú y gần người dùng")
@SecurityRequirement(name = "bearerAuth")
public class ClinicController {
    private final ClinicSearchService clinicSearchService;

    @GetMapping("/nearby")
    @Operation(summary = "Tìm phòng khám thú y gần vị trí hiện tại")
    public ResponseEntity<ApiResponse<List<NearbyClinicResponse>>> findNearbyClinics(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5") int radiusKm) {
        validateLocation(lat, lng, radiusKm);
        return ResponseEntity.ok(ApiResponse.success(
                "Tìm phòng khám gần bạn thành công",
                clinicSearchService.findNearbyClinics(lat, lng, radiusKm)
        ));
    }

    private void validateLocation(double lat, double lng, int radiusKm) {
        if (lat < -90 || lat > 90) {
            throw new BadRequestException("Vĩ độ không hợp lệ");
        }
        if (lng < -180 || lng > 180) {
            throw new BadRequestException("Kinh độ không hợp lệ");
        }
        if (radiusKm < 1 || radiusKm > 10) {
            throw new BadRequestException("Bán kính tìm kiếm phải từ 1 đến 10 km");
        }
    }
}
