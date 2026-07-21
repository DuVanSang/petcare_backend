package com.petcare.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.petcare.backend.dto.social.request.CreateSocialReportRequest;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.exception.ForbiddenException;
import com.petcare.backend.exception.ResourceNotFoundException;
import com.petcare.backend.model.Post;
import com.petcare.backend.model.PostComment;
import com.petcare.backend.model.SocialReport;
import com.petcare.backend.model.User;
import com.petcare.backend.model.enums.CommentStatus;
import com.petcare.backend.model.enums.PostStatus;
import com.petcare.backend.repository.PostCommentRepository;
import com.petcare.backend.repository.PostRepository;
import com.petcare.backend.repository.SocialReportRepository;
import com.petcare.backend.repository.UserRepository;
import com.petcare.backend.service.SocialPermissionService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SocialReportServiceImplTest {
    @Mock SocialReportRepository reports; @Mock PostRepository posts; @Mock PostCommentRepository comments; @Mock UserRepository users; @Mock SocialPermissionService permissions;
    private SocialReportServiceImpl service;
    @BeforeEach void setUp() { service = new SocialReportServiceImpl(reports, posts, comments, users, permissions); lenient().when(reports.save(any(SocialReport.class))).thenAnswer(i -> i.getArgument(0)); }
    private CreateSocialReportRequest request(SocialReport.ModerationTargetType type, Long id, SocialReport.ReportReason reason, String description) {
        CreateSocialReportRequest request = new CreateSocialReportRequest(); request.setTargetType(type); request.setTargetId(id); request.setReason(reason); request.setDescription(description); return request;
    }
    private void reporter() { User user = new User(); user.setId(1L); when(users.findById(1L)).thenReturn(Optional.of(user)); }
    private void noDuplicate() { when(reports.findByReporterIdAndTargetTypeAndTargetIdAndStatusIn(eq(1L), any(), anyLong(), any())).thenReturn(Optional.empty()); }

    @Test void validationRejectsNullRequestInvalidIdsMissingTypeAndReason() {
        assertThatThrownBy(() -> service.createReport(1L, null)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.createReport(1L, request(SocialReport.ModerationTargetType.post, null, SocialReport.ReportReason.spam, null))).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.createReport(1L, request(SocialReport.ModerationTargetType.post, 0L, SocialReport.ReportReason.spam, null))).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.createReport(1L, request(SocialReport.ModerationTargetType.post, -1L, SocialReport.ReportReason.spam, null))).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.createReport(1L, request(null, 2L, SocialReport.ReportReason.spam, null))).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.createReport(1L, request(SocialReport.ModerationTargetType.post, 2L, null, null))).isInstanceOf(BadRequestException.class);
        verify(permissions, times(6)).checkUserActive(1L); verifyNoInteractions(users, posts, comments, reports);
    }

    @Test void permissionOrMissingReporterStopsBeforeTargetLookup() {
        doThrow(new ForbiddenException("inactive")).when(permissions).checkUserActive(1L);
        assertThatThrownBy(() -> service.createReport(1L, request(SocialReport.ModerationTargetType.post, 2L, SocialReport.ReportReason.spam, null))).isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(users, posts, comments, reports);

        reset(permissions); when(users.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createReport(1L, request(SocialReport.ModerationTargetType.post, 2L, SocialReport.ReportReason.spam, null))).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(posts, comments, reports);
    }

    @Test void postTarget_handlesMissingDuplicateAndCreatesTrimmedReport() {
        reporter();
        CreateSocialReportRequest request = request(SocialReport.ModerationTargetType.post, 2L, SocialReport.ReportReason.harassment, "  details  ");
        when(posts.findByIdAndStatusNot(2L, PostStatus.DELETED)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createReport(1L, request)).isInstanceOf(ResourceNotFoundException.class);

        Post post = new Post(); when(posts.findByIdAndStatusNot(2L, PostStatus.DELETED)).thenReturn(Optional.of(post));
        SocialReport duplicate = new SocialReport();
        when(reports.findByReporterIdAndTargetTypeAndTargetIdAndStatusIn(eq(1L), eq(SocialReport.ModerationTargetType.post), eq(2L), any())).thenReturn(Optional.of(duplicate));
        assertThatThrownBy(() -> service.createReport(1L, request)).isInstanceOf(BadRequestException.class);

        when(reports.findByReporterIdAndTargetTypeAndTargetIdAndStatusIn(eq(1L), eq(SocialReport.ModerationTargetType.post), eq(2L), any())).thenReturn(Optional.empty());
        assertThat(service.createReport(1L, request).getDescription()).isEqualTo("details");
        ArgumentCaptor<SocialReport> saved = ArgumentCaptor.forClass(SocialReport.class); verify(reports).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(SocialReport.ReportStatus.pending);
        verify(permissions, times(2)).checkCanViewPost(1L, post);
    }

    @Test void commentTarget_handlesMissingDeletedAndCreatesWithBlankDescriptionAsNull() {
        reporter();
        CreateSocialReportRequest request = request(SocialReport.ModerationTargetType.comment, 3L, SocialReport.ReportReason.other, "   ");
        when(comments.findById(3L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createReport(1L, request)).isInstanceOf(ResourceNotFoundException.class);
        PostComment deleted = new PostComment(); deleted.setStatus(CommentStatus.DELETED);
        when(comments.findById(3L)).thenReturn(Optional.of(deleted));
        assertThatThrownBy(() -> service.createReport(1L, request)).isInstanceOf(ResourceNotFoundException.class);
        Post post = new Post(); PostComment visible = new PostComment(); visible.setStatus(CommentStatus.VISIBLE); visible.setPost(post);
        when(comments.findById(3L)).thenReturn(Optional.of(visible)); noDuplicate();
        assertThat(service.createReport(1L, request).getDescription()).isNull();
        verify(permissions).checkCanViewPost(1L, post); verify(reports).save(any(SocialReport.class));
    }
}
