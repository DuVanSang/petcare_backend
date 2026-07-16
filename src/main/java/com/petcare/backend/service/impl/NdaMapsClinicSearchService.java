package com.petcare.backend.service.impl;

import com.petcare.backend.dto.clinic.response.NearbyClinicResponse;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.service.ClinicSearchService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class NdaMapsClinicSearchService implements ClinicSearchService {
    private final RestClient restClient;
    private final String apiKey;
    private final String baseUrl;
    private final String category;
    private final int maxRadiusKm;
    private final int maxResults;
    private final int detailEnrichmentLimit;

    public NdaMapsClinicSearchService(
            @Value("${app.ndamaps.api-key:}") String apiKey,
            @Value("${app.ndamaps.base-url:https://mapapis.ndamaps.vn/v1}") String baseUrl,
            @Value("${app.ndamaps.clinic-category:veterinary_care}") String category,
            @Value("${app.ndamaps.max-radius-km:5}") int maxRadiusKm,
            @Value("${app.ndamaps.max-results:10}") int maxResults,
            @Value("${app.ndamaps.detail-enrichment-limit:5}") int detailEnrichmentLimit) {
        this.restClient = RestClient.create();
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.category = category;
        this.maxRadiusKm = maxRadiusKm;
        this.maxResults = maxResults;
        this.detailEnrichmentLimit = detailEnrichmentLimit;
    }

    @Override
    public List<NearbyClinicResponse> findNearbyClinics(double latitude, double longitude, int radiusKm) {
        if (!StringUtils.hasText(apiKey)) {
            throw new BadRequestException("Chưa cấu hình NDA Maps API key");
        }

        int radius = Math.min(radiusKm, maxRadiusKm);
        int size = Math.max(1, Math.min(maxResults, 20));
        try {
            Map<String, Object> response = restClient.get()
                    .uri(baseUrl + "/nearby?categories={category}&point.lat={lat}&point.lon={lon}"
                            + "&size={size}&boundary.circle.radius={radius}&apikey={apiKey}",
                            category, latitude, longitude, size, radius, apiKey)
                    .retrieve()
                    .body(Map.class);

            List<Map<String, Object>> features = response == null
                    ? List.of()
                    : (List<Map<String, Object>>) response.getOrDefault("features", List.of());

            return features.stream()
                    .map(new java.util.function.Function<Map<String, Object>, NearbyClinicResponse>() {
                        private int index = 0;

                        @Override
                        public NearbyClinicResponse apply(Map<String, Object> feature) {
                            return toClinic(feature, latitude, longitude, index++ < detailEnrichmentLimit);
                        }
                    })
                    .toList();
        } catch (RestClientException ex) {
            throw new BadRequestException("Không thể kết nối NDA Maps để tìm phòng khám");
        }
    }

    @SuppressWarnings("unchecked")
    private NearbyClinicResponse toClinic(
            Map<String, Object> feature,
            double originLat,
            double originLon,
            boolean enrichDetails) {
        Map<String, Object> geometry = (Map<String, Object>) feature.getOrDefault("geometry", Map.of());
        List<Object> coordinates = (List<Object>) geometry.getOrDefault("coordinates", List.of());
        Map<String, Object> properties = (Map<String, Object>) feature.getOrDefault("properties", Map.of());
        Map<String, Object> detailProperties = enrichDetails
                ? getPlaceDetail(asString(properties.get("id")))
                : Map.of();

        Double lon = coordinates.size() > 0 ? asDouble(coordinates.get(0)) : null;
        Double lat = coordinates.size() > 1 ? asDouble(coordinates.get(1)) : null;
        Double distanceMeters = asDouble(properties.get("distance"));
        String name = firstText(detailProperties.get("name"), properties.get("name"));
        String phone = firstText(detailProperties.get("phone"), properties.get("phone"));
        String website = firstText(detailProperties.get("website"), properties.get("website"));

        return NearbyClinicResponse.builder()
                .placeId(asString(properties.get("id")))
                .name(name)
                .address(firstText(detailProperties.get("label"), properties.get("label")))
                .shortAddress(firstText(detailProperties.get("short_address"), properties.get("short_address")))
                .phoneNumber(phone)
                .website(website)
                .latitude(lat)
                .longitude(lon)
                .distanceKm(distanceMeters != null
                        ? round(distanceMeters / 1000, 2)
                        : lat != null && lon != null ? round(haversineKm(originLat, originLon, lat, lon), 2) : null)
                .mapsUrl(buildMapsUrl(lat, lon, name))
                .source(firstText(detailProperties.get("source"), properties.get("source")))
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getPlaceDetail(String placeId) {
        if (!StringUtils.hasText(placeId)) {
            return Map.of();
        }
        try {
            Map<String, Object> response = restClient.get()
                    .uri(baseUrl + "/place?ids={placeId}&apikey={apiKey}", placeId, apiKey)
                    .retrieve()
                    .body(Map.class);
            List<Map<String, Object>> features = response == null
                    ? List.of()
                    : (List<Map<String, Object>>) response.getOrDefault("features", List.of());
            if (features.isEmpty()) {
                return Map.of();
            }
            return (Map<String, Object>) features.get(0).getOrDefault("properties", Map.of());
        } catch (RestClientException ex) {
            return Map.of();
        }
    }

    private String buildMapsUrl(Double latitude, Double longitude, String name) {
        if (latitude == null || longitude == null) {
            return null;
        }
        String query = StringUtils.hasText(name)
                ? name.replace(" ", "+") + "@" + latitude + "," + longitude
                : latitude + "," + longitude;
        return "https://www.google.com/maps/search/?api=1&query=" + query;
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private String firstText(Object primary, Object fallback) {
        String primaryText = asString(primary);
        return StringUtils.hasText(primaryText) ? primaryText : asString(fallback);
    }

    private Double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return null;
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0088;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private double round(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }
}
