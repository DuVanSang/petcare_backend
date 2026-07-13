package com.petcare.backend.service.impl;

import com.petcare.backend.dto.social.request.CreateSocialReportRequest;
import com.petcare.backend.dto.social.response.SocialReportResponse;
import com.petcare.backend.exception.BadRequestException;
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
import com.petcare.backend.service.SocialReportService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SocialReportServiceImpl implements SocialReportService {
    private final SocialReportRepository socialReportRepository;
    private final PostRepository postRepository;
    private final PostCommentRepository postCommentRepository;
    private final UserRepository userRepository;
    private final SocialPermissionService socialPermissionService;

    @Override
    @Transactional
    public SocialReportResponse createReport(Long currentUserId, CreateSocialReportRequest request) {
        socialPermissionService.checkUserActive(currentUserId);
        validateRequest(request);
        User reporter = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        validateTargetVisibleToReporter(currentUserId, request.getTargetType(), request.getTargetId());

        socialReportRepository.findByReporterIdAndTargetTypeAndTargetIdAndStatusIn(
                currentUserId,
                request.getTargetType(),
                request.getTargetId(),
                List.of(SocialReport.ReportStatus.pending, SocialReport.ReportStatus.reviewing)
        ).ifPresent(report -> {
            throw new BadRequestException("Bạn đã báo cáo nội dung này và báo cáo đang chờ xử lý");
        });

        SocialReport report = new SocialReport();
        report.setReporter(reporter);
        report.setTargetType(request.getTargetType());
        report.setTargetId(request.getTargetId());
        report.setReason(request.getReason());
        report.setDescription(trimToNull(request.getDescription()));
        report.setStatus(SocialReport.ReportStatus.pending);
        return SocialReportResponse.from(socialReportRepository.save(report));
    }

    private void validateRequest(CreateSocialReportRequest request) {
        if (request == null) {
            throw new BadRequestException("Dữ liệu báo cáo không được để trống");
        }
        if (request.getTargetId() == null || request.getTargetId() <= 0) {
            throw new BadRequestException("Id đối tượng bị báo cáo phải lớn hơn 0");
        }
        if (request.getTargetType() == null) {
            throw new BadRequestException("Loại đối tượng bị báo cáo không hợp lệ");
        }
        if (request.getReason() == null) {
            throw new BadRequestException("Lý do báo cáo không hợp lệ");
        }
    }

    private void validateTargetVisibleToReporter(
            Long currentUserId,
            SocialReport.ModerationTargetType targetType,
            Long targetId
    ) {
        if (targetType == SocialReport.ModerationTargetType.post) {
            Post post = postRepository.findByIdAndStatusNot(targetId, PostStatus.DELETED)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết"));
            socialPermissionService.checkCanViewPost(currentUserId, post);
            return;
        }

        PostComment comment = postCommentRepository.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bình luận"));
        if (comment.getStatus() == CommentStatus.DELETED) {
            throw new ResourceNotFoundException("Không tìm thấy bình luận");
        }
        socialPermissionService.checkCanViewPost(currentUserId, comment.getPost());
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
