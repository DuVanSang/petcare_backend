package com.petcare.backend.service;

import com.petcare.backend.dto.emr.request.CreateEmrRecordRequest;
import com.petcare.backend.dto.emr.response.EmrRecordResponse;
import com.petcare.backend.security.UserPrincipal;

import java.util.List;

public interface EmrRecordService {

    EmrRecordResponse createEmrRecord(UserPrincipal principal, CreateEmrRecordRequest request);

    List<EmrRecordResponse> getEmrRecordsByPet(UserPrincipal principal, Long petId);

    EmrRecordResponse getEmrRecordById(UserPrincipal principal, Long emrRecordId);
}
