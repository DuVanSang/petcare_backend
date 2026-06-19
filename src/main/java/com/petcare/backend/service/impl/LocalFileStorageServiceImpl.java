package com.petcare.backend.service.impl;

import com.petcare.backend.dto.upload.UploadFileResponse;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.service.FileStorageService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class LocalFileStorageServiceImpl implements FileStorageService {
    private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MM");
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

    @Value("${app.upload.root-dir:uploads}")
    private String uploadRootDir;

    @Value("${app.upload.public-url-prefix:http://localhost:9090/uploads}")
    private String publicUrlPrefix;

    @Value("${app.upload.post-media-dir:post-media}")
    private String postMediaDir;

    @Value("${app.upload.comment-media-dir:comment-media}")
    private String commentMediaDir;

    @Value("${app.upload.max-file-size-mb:20}")
    private long maxFileSizeMb;

    @Override
    public UploadFileResponse storePostMediaFile(MultipartFile file) {
        return storeMediaFile(file, postMediaDir);
    }

    @Override
    public List<UploadFileResponse> storePostMediaFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        return files.stream()
                .map(this::storePostMediaFile)
                .toList();
    }

    @Override
    public UploadFileResponse storeCommentMediaFile(MultipartFile file) {
        return storeMediaFile(file, commentMediaDir);
    }

    @Override
    public List<UploadFileResponse> storeCommentMediaFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        return files.stream()
                .map(this::storeCommentMediaFile)
                .toList();
    }

    private UploadFileResponse storeMediaFile(MultipartFile file, String mediaDirectory) {
        validateFile(file);

        String originalFilename = file.getOriginalFilename().trim();
        String mimeType = file.getContentType().toLowerCase(Locale.ROOT);
        String mediaType = SUPPORTED_MIME_TYPES.get(mimeType);
        String storedFilename = UUID.randomUUID() + "-" + sanitizeFilename(originalFilename);

        LocalDate now = LocalDate.now();
        String year = YEAR_FORMATTER.format(now);
        String month = MONTH_FORMATTER.format(now);

        Path targetDirectory = Paths.get(uploadRootDir, mediaDirectory, year, month)
                .toAbsolutePath()
                .normalize();
        Path targetPath = targetDirectory.resolve(storedFilename).normalize();

        if (!targetPath.startsWith(targetDirectory)) {
            throw new BadRequestException("Invalid filename");
        }

        try {
            Files.createDirectories(targetDirectory);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not store file", ex);
        }

        String mediaUrl = joinUrl(publicUrlPrefix, mediaDirectory, year, month, storedFilename);
        return UploadFileResponse.builder()
                .mediaType(mediaType)
                .mediaUrl(mediaUrl)
                .thumbnailUrl(null)
                .originalFilename(originalFilename)
                .storedFilename(storedFilename)
                .mimeType(mimeType)
                .fileSize(file.getSize())
                .build();
    }

    private void validateFile(MultipartFile file) {
        if (file == null) {
            throw new BadRequestException("File is required");
        }
        if (file.isEmpty()) {
            throw new BadRequestException("File must not be empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)) {
            throw new BadRequestException("Original filename is required");
        }
        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new BadRequestException("Invalid filename");
        }

        long maxFileSizeBytes = maxFileSizeMb * 1024 * 1024;
        if (file.getSize() > maxFileSizeBytes) {
            throw new BadRequestException("File exceeds maximum size");
        }

        String mimeType = file.getContentType();
        if (!StringUtils.hasText(mimeType)
                || !SUPPORTED_MIME_TYPES.containsKey(mimeType.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("Unsupported file type");
        }
    }

    private String sanitizeFilename(String filename) {
        String sanitized = filename.trim().replaceAll("\\s+", "-");
        sanitized = sanitized.replaceAll("[^A-Za-z0-9._-]", "");
        if (!StringUtils.hasText(sanitized)) {
            throw new BadRequestException("Invalid filename");
        }
        return sanitized;
    }

    private String joinUrl(String baseUrl, String... paths) {
        StringBuilder builder = new StringBuilder(baseUrl.replaceAll("/+$", ""));
        for (String path : paths) {
            builder.append('/').append(path.replaceAll("^/+", "").replaceAll("/+$", ""));
        }
        return builder.toString();
    }
}
