package com.petcare.backend.dto.post.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePostRequest {
    private Long petId;
    @JsonIgnore
    private boolean petIdSet;

    @Size(max = 3000, message = "Caption must not exceed 3000 characters")
    private String caption;

    private String privacy;

    private Boolean commentsLocked;

    @Valid
    @Size(max = 10, message = "A post can contain at most 10 media items")
    private List<CreatePostMediaRequest> media;

    public void setPetId(Long petId) {
        this.petId = petId;
        this.petIdSet = true;
    }

    @JsonSetter("petId")
    public void setPetIdFromJson(Long petId) {
        setPetId(petId);
    }
}
