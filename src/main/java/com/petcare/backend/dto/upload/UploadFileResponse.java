package com.petcare.backend.dto.upload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadFileResponse {
    private String mediaType;
    private String mediaUrl;
    private String thumbnailUrl;
    private String originalFilename;
    private String storedFilename;
    private String mimeType;
    private Long fileSize;
}
