package com.petcare.backend.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.petcare.backend.dto.upload.UploadFileResponse;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.service.FileStorageService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@Primary
@ConditionalOnProperty(name = "storage.provider", havingValue = "cloudinary", matchIfMissing = false)
@RequiredArgsConstructor
public class CloudinaryFileStorageServiceImpl implements FileStorageService {

    private static final Map<String, String> SUPPORTED_MIME_TYPES = Map.ofEntries(
            Map.entry("image/jpeg", "image"),
            Map.entry("image/png", "image"),
            Map.entry("image/webp", "image"),
            Map.entry("image/gif", "image"),
            Map.entry("video/mp4", "video"),
            Map.entry("video/webm", "video"),
            Map.entry("video/quicktime", "video"),
            Map.entry("application/pdf", "document"),
            Map.entry("application/msword", "document"),
            Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "document"),
            Map.entry("text/plain", "document")
    );

    private final Cloudinary cloudinary;

    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("🚀 [STORAGE] Đã kích hoạt CLOUDINARY CLOUD STORAGE thành công!");
    }

    @Override
    public UploadFileResponse storePostMediaFile(MultipartFile file) {
        return uploadToCloudinary(file, "posts");
    }

    @Override
    public List<UploadFileResponse> storePostMediaFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        List<UploadFileResponse> responses = new ArrayList<>();
        for (MultipartFile file : files) {
            responses.add(uploadToCloudinary(file, "posts"));
        }
        return responses;
    }

    @Override
    public UploadFileResponse storeCommentMediaFile(MultipartFile file) {
        return uploadToCloudinary(file, "comments");
    }

    @Override
    public List<UploadFileResponse> storeCommentMediaFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        List<UploadFileResponse> responses = new ArrayList<>();
        for (MultipartFile file : files) {
            responses.add(uploadToCloudinary(file, "comments"));
        }
        return responses;
    }

    @Override
    public UploadFileResponse storePetAvatar(MultipartFile file, Long userId) {
        validateProfileImage(file, "Ảnh đại diện thú cưng không hợp lệ");
        return uploadToCloudinary(file, "pets");
    }

    @Override
    public UploadFileResponse storeUserProfileImage(MultipartFile file, Long userId, String imageType) {
        validateProfileImage(file, "Ảnh hồ sơ không hợp lệ");
        String normalizedType = switch (imageType) {
            case "avatar" -> "avatar";
            case "cover" -> "cover";
            default -> throw new BadRequestException("Loại ảnh hồ sơ không hợp lệ");
        };
        return uploadToCloudinary(file, "users/" + normalizedType);
    }

    @Override
    public UploadFileResponse storeBlogCoverImage(MultipartFile file) {
        validateProfileImage(file, "Ảnh bìa blog không hợp lệ. Chỉ hỗ trợ JPG, PNG, WEBP và tối đa 5MB");
        return uploadToCloudinary(file, "blogs");
    }

    @Override
    public UploadFileResponse storeMomentMediaFile(MultipartFile file) {
        return uploadToCloudinary(file, "moments");
    }

    @Override
    public void deleteByUrl(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) return;
        try {
            // Extract publicId if it is a cloudinary url
            if (fileUrl.contains("res.cloudinary.com")) {
                int uploadIndex = fileUrl.indexOf("/upload/");
                if (uploadIndex != -1) {
                    String pathAfterUpload = fileUrl.substring(uploadIndex + 8);
                    // Remove version like v1234567890/
                    String withoutVersion = pathAfterUpload.replaceFirst("^v\\d+/", "");
                    // Remove extension (.jpg, .png, etc.)
                    int dotIndex = withoutVersion.lastIndexOf('.');
                    String publicId = dotIndex != -1 ? withoutVersion.substring(0, dotIndex) : withoutVersion;
                    cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                }
            }
        } catch (Exception ex) {
            log.warn("Không thể xóa ảnh trên Cloudinary: {}", ex.getMessage());
        }
    }

    private UploadFileResponse uploadToCloudinary(MultipartFile file, String folder) {
        validateFile(file);

        String mimeType = resolveMimeType(file);
        String mediaType = SUPPORTED_MIME_TYPES.getOrDefault(mimeType, "image");

        String publicId = UUID.randomUUID().toString();

        try {
            Map<?, ?> uploadParams = ObjectUtils.asMap(
                    "folder", "petcare/" + folder,
                    "public_id", publicId,
                    "resource_type", "auto",
                    "quality", "auto",
                    "fetch_format", "auto"
            );

            log.info("☁️ Đang tải ảnh '{}' lên Cloudinary folder 'petcare/{}'...", file.getOriginalFilename(), folder);
            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);
            String secureUrl = (String) uploadResult.get("secure_url");
            log.info("✅ Tải ảnh lên Cloudinary THÀNH CÔNG! Link: {}", secureUrl);

            return UploadFileResponse.builder()
                    .mediaType(mediaType)
                    .mediaUrl(secureUrl)
                    .thumbnailUrl(secureUrl)
                    .originalFilename(file.getOriginalFilename())
                    .storedFilename(publicId)
                    .mimeType(mimeType)
                    .fileSize(file.getSize())
                    .build();

        } catch (IOException ex) {
            log.error("Lỗi upload ảnh lên Cloudinary: {}", ex.getMessage(), ex);
            throw new BadRequestException("Không thể tải ảnh lên máy chủ Cloudinary: " + ex.getMessage());
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File không được để trống");
        }

        String mimeType = resolveMimeType(file);
        if (!SUPPORTED_MIME_TYPES.containsKey(mimeType)) {
            throw new BadRequestException("Định dạng file không được hỗ trợ");
        }

        long maxBytes = 20L * 1024 * 1024; // 20MB
        if (file.getSize() > maxBytes) {
            throw new BadRequestException("Kích thước file vượt quá giới hạn 20MB");
        }
    }

    private void validateProfileImage(MultipartFile file, String message) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException(message);
        }
        String normalizedMime = resolveMimeType(file);
        if (!normalizedMime.equals("image/jpeg") && !normalizedMime.equals("image/png") && !normalizedMime.equals("image/webp")) {
            throw new BadRequestException("Chỉ hỗ trợ file ảnh JPG, PNG hoặc WEBP");
        }
        if (file.getSize() > 5L * 1024 * 1024) {
            throw new BadRequestException("Kích thước ảnh không được vượt quá 5MB");
        }
    }

    private String resolveMimeType(MultipartFile file) {
        String mimeType = file.getContentType();
        if (StringUtils.hasText(mimeType) && !"application/octet-stream".equalsIgnoreCase(mimeType)) {
            return normalizeMimeType(mimeType);
        }

        String filename = file.getOriginalFilename();
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        String lowerFilename = filename.toLowerCase(Locale.ROOT);
        if (lowerFilename.endsWith(".jpg") || lowerFilename.endsWith(".jpeg")) return "image/jpeg";
        if (lowerFilename.endsWith(".png")) return "image/png";
        if (lowerFilename.endsWith(".webp")) return "image/webp";
        if (lowerFilename.endsWith(".gif")) return "image/gif";
        if (lowerFilename.endsWith(".mp4")) return "video/mp4";
        if (lowerFilename.endsWith(".webm")) return "video/webm";
        if (lowerFilename.endsWith(".mov")) return "video/quicktime";
        if (lowerFilename.endsWith(".pdf")) return "application/pdf";
        if (lowerFilename.endsWith(".doc")) return "application/msword";
        if (lowerFilename.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lowerFilename.endsWith(".txt")) return "text/plain";
        return "";
    }

    private String normalizeMimeType(String mimeType) {
        String normalized = mimeType.toLowerCase(Locale.ROOT);
        if ("image/jpg".equals(normalized) || "image/pjpeg".equals(normalized)) {
            return "image/jpeg";
        }
        return normalized;
    }
}
