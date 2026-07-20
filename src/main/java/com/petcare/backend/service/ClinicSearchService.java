package com.petcare.backend.service;

import com.petcare.backend.dto.clinic.response.NearbyClinicResponse;
import java.util.List;

public interface ClinicSearchService {
    List<NearbyClinicResponse> findNearbyClinics(double latitude, double longitude, int radiusKm);
}
