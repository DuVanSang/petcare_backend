package com.petcare.backend.dto.admin.category.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUpdateBreedRequest {
    private Long speciesId;

    @Size(max = 100, message = "Tên giống không được quá 100 ký tự")
    private String name;

    private Boolean active;
}
