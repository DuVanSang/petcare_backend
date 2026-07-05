package com.petcare.backend.dto.blog.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BlogOptionResponse {
    private String value;
    private String label;
}
