package com.petcare.backend.dto.clinic.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NearbyClinicResponse {
    private String placeId;
    private String name;
    private String address;
    private String shortAddress;
    private String phoneNumber;
    private String website;
    private Double latitude;
    private Double longitude;
    private Double distanceKm;
    private String mapsUrl;
    private String source;
}
