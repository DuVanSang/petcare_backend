package com.petcare.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.petcare.backend.dto.upload.UploadFileResponse;
import com.petcare.backend.exception.BadRequestException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

class LocalFileStorageServiceImplTest {
    @TempDir Path storageRoot;
    private LocalFileStorageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LocalFileStorageServiceImpl();
        ReflectionTestUtils.setField(service, "uploadRootDir", storageRoot.toString());
        ReflectionTestUtils.setField(service, "publicUrlPrefix", "https://files.test/uploads/");
        ReflectionTestUtils.setField(service, "postMediaDir", "post-media");
        ReflectionTestUtils.setField(service, "commentMediaDir", "comment-media");
        ReflectionTestUtils.setField(service, "petAvatarDir", "pet-avatar");
        ReflectionTestUtils.setField(service, "userProfileDir", "user-profile");
        ReflectionTestUtils.setField(service, "maxFileSizeMb", 1L);
    }

    private MockMultipartFile file(String name, String contentType, byte[] content) {
        return new MockMultipartFile("file", name, contentType, content);
    }

    @Test
    void storePostMediaFile_CreatesDirectoryAndReturnsSafePublicUrl() throws IOException {
        UploadFileResponse response = service.storePostMediaFile(
                file("  my.photo.final.txt  ", "text/plain", "content".getBytes()));

        assertThat(response.getMediaType()).isEqualTo("document");
        assertThat(response.getOriginalFilename()).isEqualTo("my.photo.final.txt");
        assertThat(response.getStoredFilename()).endsWith("-my.photo.final.txt");
        assertThat(response.getMediaUrl()).startsWith("https://files.test/uploads/post-media/");
        try (var paths = Files.walk(storageRoot)) {
            assertThat(paths.filter(Files::isRegularFile).map(Path::getFileName)
                    .map(Path::toString).toList()).contains(response.getStoredFilename());
        }
    }

    @Test
    void storeCommentAndCollectionMethods_HandleEmptyAndMultipleFiles() {
        assertThat(service.storePostMediaFiles(null)).isEmpty();
        assertThat(service.storeCommentMediaFiles(List.of())).isEmpty();

        UploadFileResponse comment = service.storeCommentMediaFile(file("note", "text/plain", new byte[] {1}));
        List<UploadFileResponse> posts = service.storePostMediaFiles(List.of(
                file("one.png", "image/png", new byte[] {1}),
                file("two.gif", "image/gif", new byte[] {2})));

        assertThat(comment.getMediaUrl()).contains("/comment-media/");
        assertThat(posts).hasSize(2);
        assertThat(posts).extracting(UploadFileResponse::getMediaType).containsExactly("image", "image");
    }

    @Test
    void profileImages_UseUuidExtensionAndRejectInvalidInputs() {
        UploadFileResponse avatar = service.storePetAvatar(file("pet.any", "image/png", new byte[] {1}), 99L);
        UploadFileResponse cover = service.storeUserProfileImage(file("cover.jpg", "image/jpeg", new byte[] {1}), 99L, "cover");
        UploadFileResponse jpgAlias = service.storeUserProfileImage(file("alias.jpg", "image/jpg", new byte[] {1}), 99L, "avatar");
        UploadFileResponse inferred = service.storeUserProfileImage(file("camera.jpeg", "application/octet-stream", new byte[] {1}), 99L, "avatar");

        assertThat(avatar.getStoredFilename()).matches("[0-9a-f-]+\\.png");
        assertThat(cover.getMediaUrl()).contains("/user-profile/cover/");
        assertThat(jpgAlias.getMimeType()).isEqualTo("image/jpeg");
        assertThat(inferred.getStoredFilename()).endsWith(".jpg");
        assertThatThrownBy(() -> service.storeUserProfileImage(file("x.gif", "image/gif", new byte[] {1}), 1L, "avatar"))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.storeUserProfileImage(file("x.png", "image/png", new byte[] {1}), 1L, "banner"))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.storePetAvatar(null, 1L)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void profileAvatarAndCommentCollection_CoverNonEmptyAndSanitizationBranches() {
        UploadFileResponse avatar = service.storeUserProfileImage(
                file("avatar.webp", "image/webp", new byte[] {1}), 3L, "avatar");
        List<UploadFileResponse> comments = service.storeCommentMediaFiles(List.of(
                file("comment.txt", "text/plain", new byte[] {1})));

        assertThat(avatar.getStoredFilename()).endsWith(".webp");
        assertThat(comments).hasSize(1);
        assertThatThrownBy(() -> service.storePostMediaFile(file("$$$", "text/plain", new byte[] {1})))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void storeMedia_RejectsNullEmptyInvalidNamesTraversalSizeAndMimeType() {
        assertThatThrownBy(() -> service.storePostMediaFile(null)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.storePostMediaFile(file("x.txt", "text/plain", new byte[0])))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.storePostMediaFile(file(" ", "text/plain", new byte[] {1})))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.storePostMediaFile(file("../escape.txt", "text/plain", new byte[] {1})))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.storePostMediaFile(file("sub/file.txt", "text/plain", new byte[] {1})))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.storePostMediaFile(file("x.bin", "application/octet-stream", new byte[] {1})))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.storePostMediaFile(file("big.txt", "text/plain", new byte[1024 * 1024 + 1])))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void deleteByUrl_DeletesOnlyFilesInsideConfiguredRoot() throws IOException {
        UploadFileResponse stored = service.storePostMediaFile(file("delete.txt", "text/plain", new byte[] {1}));
        Path target;
        try (var paths = Files.walk(storageRoot)) {
            target = paths.filter(Files::isRegularFile).findFirst().orElseThrow();
        }

        service.deleteByUrl(stored.getMediaUrl());
        assertThat(Files.exists(target)).isFalse();
        assertDoesNotThrow(() -> service.deleteByUrl(stored.getMediaUrl()));
        assertDoesNotThrow(() -> service.deleteByUrl(null));
        assertDoesNotThrow(() -> service.deleteByUrl("https://elsewhere.test/file.txt"));
        assertDoesNotThrow(() -> service.deleteByUrl("https://files.test/uploads/../outside.txt"));
        try (var paths = Files.walk(storageRoot)) {
            assertThat(paths.filter(Files::isRegularFile).toList()).isEmpty();
        }
    }

    @Test
    void copyIOException_IsWrappedWithoutCreatingFileOutsideTempRoot() throws IOException {
        MultipartFile failing = Mockito.mock(MultipartFile.class);
        Mockito.when(failing.isEmpty()).thenReturn(false);
        Mockito.when(failing.getOriginalFilename()).thenReturn("broken.txt");
        Mockito.when(failing.getContentType()).thenReturn("text/plain");
        Mockito.when(failing.getSize()).thenReturn(1L);
        Mockito.when(failing.getInputStream()).thenThrow(new IOException("read failed"));

        assertThatThrownBy(() -> service.storePostMediaFile(failing)).isInstanceOf(java.io.UncheckedIOException.class);
        try (var paths = Files.walk(storageRoot)) {
            assertThat(paths.filter(Files::isRegularFile).toList()).isEmpty();
        }
    }
}
