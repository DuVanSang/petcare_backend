package com.petcare.backend.dto.vaccination.request;

import com.petcare.backend.dto.pet.request.VaccinationHistoryRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SetupVaccinationPlanRequest {
    @Valid
    @NotEmpty(message = "Vui lòng cung cấp lịch sử tiêm của thú cưng")
    private List<VaccinationHistoryRequest> histories;
}
