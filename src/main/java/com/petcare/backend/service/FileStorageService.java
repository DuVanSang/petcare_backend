package com.petcare.backend.service;

import com.petcare.backend.dto.upload.UploadFileResponse;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    UploadFileResponse storePostMediaFile(MultipartFile file);

    List<UploadFileResponse> storePostMediaFiles(List<MultipartFile> files);

    UploadFileResponse storeCommentMediaFile(MultipartFile file);

    List<UploadFileResponse> storeCommentMediaFiles(List<MultipartFile> files);
}
