package com.petcare.backend.dto.vaccination.response;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VaccinationSafetyWarningResponse {
    private boolean warning;
    private WarningLevel warningLevel;
    private String title;
    private String message;
    private List<String> reasons;
    private LocalDate latestHealthLogDate;

    public enum WarningLevel {
        none, caution, high
    }
}
