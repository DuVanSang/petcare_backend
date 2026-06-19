package com.petcare.backend.dto.health.response;

import com.petcare.backend.model.PetTimelineEvent;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class TimelineEventResponse {

    private Long id;
    private Long petId;
    private PetTimelineEvent.EventType eventType;
    private Long referenceId;
    private LocalDate eventDate;
    private String summary;
    private LocalDateTime createdAt;

    public static TimelineEventResponse from(PetTimelineEvent event) {
        return TimelineEventResponse.builder()
                .id(event.getId())
                .petId(event.getPet().getId())
                .eventType(event.getEventType())
                .referenceId(event.getReferenceId())
                .eventDate(event.getEventDate())
                .summary(event.getSummary())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
