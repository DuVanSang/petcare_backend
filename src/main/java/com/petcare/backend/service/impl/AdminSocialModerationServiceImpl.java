package com.petcare.backend.service.impl;

import com.petcare.backend.dto.admin.social.request.ModerationActionRequest;
import com.petcare.backend.dto.admin.social.request.ResolveSocialReportRequest;
import com.petcare.backend.dto.admin.social.response.AdminSocialCommentResponse;
import com.petcare.backend.dto.admin.social.response.AdminSocialPostResponse;
import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.dto.social.response.SocialReportResponse;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.exception.ResourceNotFoundException;
import com.petcare.backend.model.ModerationAction;
import com.petcare.backend.model.Pet;
import com.petcare.backend.model.Post;
import com.petcare.backend.model.PostComment;
import com.petcare.backend.model.PostMedia;
import com.petcare.backend.model.SocialReport;
import com.petcare.backend.model.User;
import com.petcare.backend.model.enums.CommentStatus;
import com.petcare.backend.model.enums.PostPrivacy;
import com.petcare.backend.model.enums.PostStatus;
import com.petcare.backend.repository.CommentReactionRepository;
import com.petcare.backend.repository.ModerationActionRepository;
import com.petcare.backend.repository.PostCommentRepository;
import com.petcare.backend.repository.PostMediaRepository;
import com.petcare.backend.repository.PostReactionRepository;
import com.petcare.backend.repository.PostRepository;
import com.petcare.backend.repository.PetRepository;
import com.petcare.backend.repository.SocialReportRepository;
import com.petcare.backend.repository.UserRepository;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.AdminSocialModerationService;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminSocialModerationServiceImpl implements AdminSocialModerationService {
    private static final int MAX_PAGE_SIZE = 100;

    private final PostRepository postRepository;
    private final PostMediaRepository postMediaRepository;
    private final PostCommentRepository postCommentRepository;
    private final PostReactionRepository postReactionRepository;
    private final CommentReactionRepository commentReactionRepository;
    private final PetRepository petRepository;
    private final SocialReportRepository socialReportRepository;
    private final ModerationActionRepository moderationActionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminSocialPostResponse> getPosts(
            String keyword,
            Long authorId,
            Long petId,
            PostStatus status,
            PostPrivacy privacy,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size
    ) {
        validateTimeRange(from, to);
        Pageable pageable = PageRequest.of(validatePage(page), validateSize(size), Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(postRepository
                .findAll(postSpecification(keyword, authorId, petId, status, privacy, from, to), pageable)
                .map(this::toPostResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminSocialPostResponse getPostDetail(Long postId) {
        return toPostResponse(getPostOrThrow(postId));
    }

    @Override
    @Transactional
    public AdminSocialPostResponse hidePost(UserPrincipal moderator, Long postId, ModerationActionRequest request) {
        Post post = getPostOrThrow(postId);
        if (post.getStatus() == PostStatus.DELETED) {
            throw new BadRequestException("Không thể ẩn bài viết đã bị xóa");
        }
        post.setStatus(PostStatus.HIDDEN);
        recordAction(
                SocialReport.ModerationTargetType.post,
                postId,
                ModerationAction.ModerationActionType.hide,
                reasonFrom(request),
                getModerator(moderator),
                null
        );
        return toPostResponse(postRepository.save(post));
    }

    @Override
    @Transactional
    public AdminSocialPostResponse restorePost(UserPrincipal moderator, Long postId, ModerationActionRequest request) {
        Post post = getPostOrThrow(postId);
        if (post.getStatus() == PostStatus.DELETED) {
            throw new BadRequestException("Không thể khôi phục bài viết đã bị xóa");
        }
        post.setStatus(PostStatus.PUBLISHED);
        recordAction(
                SocialReport.ModerationTargetType.post,
                postId,
                ModerationAction.ModerationActionType.restore,
                reasonFrom(request),
                getModerator(moderator),
                null
        );
        return toPostResponse(postRepository.save(post));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminSocialCommentResponse> getComments(
            String keyword,
            Long postId,
            Long authorId,
            CommentStatus status,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size
    ) {
        validateTimeRange(from, to);
        Pageable pageable = PageRequest.of(validatePage(page), validateSize(size), Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(postCommentRepository
                .findAll(commentSpecification(keyword, postId, authorId, status, from, to), pageable)
                .map(this::toCommentResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminSocialCommentResponse getCommentDetail(Long commentId) {
        return toCommentResponse(getCommentOrThrow(commentId));
    }

    @Override
    @Transactional
    public AdminSocialCommentResponse hideComment(UserPrincipal moderator, Long commentId, ModerationActionRequest request) {
        PostComment comment = getCommentOrThrow(commentId);
        if (comment.getStatus() == CommentStatus.DELETED) {
            throw new BadRequestException("Không thể ẩn bình luận đã bị xóa");
        }
        comment.setStatus(CommentStatus.HIDDEN);
        recordAction(
                SocialReport.ModerationTargetType.comment,
                commentId,
                ModerationAction.ModerationActionType.hide,
                reasonFrom(request),
                getModerator(moderator),
                null
        );
        return toCommentResponse(postCommentRepository.save(comment));
    }

    @Override
    @Transactional
    public AdminSocialCommentResponse restoreComment(UserPrincipal moderator, Long commentId, ModerationActionRequest request) {
        PostComment comment = getCommentOrThrow(commentId);
        if (comment.getStatus() == CommentStatus.DELETED) {
            throw new BadRequestException("Không thể khôi phục bình luận đã bị xóa");
        }
        comment.setStatus(CommentStatus.VISIBLE);
        recordAction(
                SocialReport.ModerationTargetType.comment,
                commentId,
                ModerationAction.ModerationActionType.restore,
                reasonFrom(request),
                getModerator(moderator),
                null
        );
        return toCommentResponse(postCommentRepository.save(comment));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SocialReportResponse> getReports(
            SocialReport.ReportStatus status,
            SocialReport.ModerationTargetType targetType,
            SocialReport.ReportReason reason,
            Long reporterId,
            Long targetId,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size
    ) {
        validateTimeRange(from, to);
        Pageable pageable = PageRequest.of(validatePage(page), validateSize(size), Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(socialReportRepository
                .findAll(reportSpecification(status, targetType, reason, reporterId, targetId, from, to), pageable)
                .map(this::toReportResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public SocialReportResponse getReportDetail(Long reportId) {
        return toReportResponse(getReportOrThrow(reportId));
    }

    @Override
    @Transactional
    public SocialReportResponse resolveReport(UserPrincipal moderator, Long reportId, ResolveSocialReportRequest request) {
        SocialReport report = getPendingReportOrThrow(reportId);
        User moderatorUser = getModerator(moderator);
        if (request != null && Boolean.TRUE.equals(request.getHideTarget())) {
            hideReportedTarget(report);
            recordAction(
                    report.getTargetType(),
                    report.getTargetId(),
                    ModerationAction.ModerationActionType.hide,
                    trimToNull(request.getResolutionNote()),
                    moderatorUser,
                    report
            );
        }
        report.setStatus(SocialReport.ReportStatus.resolved);
        report.setResolvedBy(moderatorUser);
        report.setResolutionNote(request == null ? null : trimToNull(request.getResolutionNote()));
        report.setResolvedAt(LocalDateTime.now());
        recordAction(
                report.getTargetType(),
                report.getTargetId(),
                ModerationAction.ModerationActionType.resolve_report,
                report.getResolutionNote(),
                moderatorUser,
                report
        );
        return toReportResponse(socialReportRepository.save(report));
    }

    @Override
    @Transactional
    public SocialReportResponse rejectReport(UserPrincipal moderator, Long reportId, ResolveSocialReportRequest request) {
        SocialReport report = getPendingReportOrThrow(reportId);
        User moderatorUser = getModerator(moderator);
        report.setStatus(SocialReport.ReportStatus.rejected);
        report.setResolvedBy(moderatorUser);
        report.setResolutionNote(request == null ? null : trimToNull(request.getResolutionNote()));
        report.setResolvedAt(LocalDateTime.now());
        recordAction(
                report.getTargetType(),
                report.getTargetId(),
                ModerationAction.ModerationActionType.reject_report,
                report.getResolutionNote(),
                moderatorUser,
                report
        );
        return toReportResponse(socialReportRepository.save(report));
    }

    private Specification<Post> postSpecification(
            String keyword,
            Long authorId,
            Long petId,
            PostStatus status,
            PostPrivacy privacy,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                predicates.add(cb.like(cb.lower(root.get("caption")), "%" + keyword.trim().toLowerCase() + "%"));
            }
            if (authorId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), authorId));
            }
            if (petId != null) {
                predicates.add(cb.equal(root.get("petId"), petId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (privacy != null) {
                predicates.add(cb.equal(root.get("privacy"), privacy));
            }
            addCreatedRange(predicates, root.get("createdAt"), cb, from, to);
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Specification<PostComment> commentSpecification(
            String keyword,
            Long postId,
            Long authorId,
            CommentStatus status,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                predicates.add(cb.like(cb.lower(root.get("commentText")), "%" + keyword.trim().toLowerCase() + "%"));
            }
            if (postId != null) {
                predicates.add(cb.equal(root.get("post").get("id"), postId));
            }
            if (authorId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), authorId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            addCreatedRange(predicates, root.get("createdAt"), cb, from, to);
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Specification<SocialReport> reportSpecification(
            SocialReport.ReportStatus status,
            SocialReport.ModerationTargetType targetType,
            SocialReport.ReportReason reason,
            Long reporterId,
            Long targetId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (targetType != null) {
                predicates.add(cb.equal(root.get("targetType"), targetType));
            }
            if (reason != null) {
                predicates.add(cb.equal(root.get("reason"), reason));
            }
            if (reporterId != null) {
                predicates.add(cb.equal(root.get("reporter").get("id"), reporterId));
            }
            if (targetId != null) {
                predicates.add(cb.equal(root.get("targetId"), targetId));
            }
            addCreatedRange(predicates, root.get("createdAt"), cb, from, to);
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void hideReportedTarget(SocialReport report) {
        if (report.getTargetType() == SocialReport.ModerationTargetType.post) {
            Post post = getPostOrThrow(report.getTargetId());
            if (post.getStatus() != PostStatus.DELETED) {
                post.setStatus(PostStatus.HIDDEN);
                postRepository.save(post);
            }
            return;
        }

        PostComment comment = getCommentOrThrow(report.getTargetId());
        if (comment.getStatus() != CommentStatus.DELETED) {
            comment.setStatus(CommentStatus.HIDDEN);
            postCommentRepository.save(comment);
        }
    }

    private AdminSocialPostResponse toPostResponse(Post post) {
        String petName = post.getPetId() == null
                ? null
                : petRepository.findById(post.getPetId())
                        .map(Pet::getName)
                        .orElse(null);
        List<String> mediaUrls = postMediaRepository.findByPost_IdOrderByDisplayOrderAsc(post.getId())
                .stream()
                .map(PostMedia::getMediaUrl)
                .toList();
        return AdminSocialPostResponse.from(
                post,
                postReactionRepository.countByPost_Id(post.getId()),
                postCommentRepository.countByPost_IdAndStatus(post.getId(), CommentStatus.VISIBLE),
                petName,
                mediaUrls
        );
    }

    private SocialReportResponse toReportResponse(SocialReport report) {
        String targetAuthorName = null;
        String targetAuthorEmail = null;
        String targetPreview = null;
        List<String> targetMediaUrls = List.of();

        if (report.getTargetType() == SocialReport.ModerationTargetType.post) {
            Post post = postRepository.findById(report.getTargetId()).orElse(null);
            if (post != null) {
                if (post.getUser() != null) {
                    targetAuthorName = post.getUser().getFullName();
                    targetAuthorEmail = post.getUser().getEmail();
                }
                targetPreview = post.getCaption();
                targetMediaUrls = postMediaRepository.findByPost_IdOrderByDisplayOrderAsc(post.getId())
                        .stream()
                        .map(PostMedia::getMediaUrl)
                        .toList();
            }
        } else if (report.getTargetType() == SocialReport.ModerationTargetType.comment) {
            PostComment comment = postCommentRepository.findById(report.getTargetId()).orElse(null);
            if (comment != null) {
                if (comment.getUser() != null) {
                    targetAuthorName = comment.getUser().getFullName();
                    targetAuthorEmail = comment.getUser().getEmail();
                }
                targetPreview = comment.getCommentText();
            }
        }

        return SocialReportResponse.builder()
                .id(report.getId())
                .targetType(report.getTargetType() == null ? null : report.getTargetType().name())
                .targetId(report.getTargetId())
                .targetAuthorName(targetAuthorName)
                .targetAuthorEmail(targetAuthorEmail)
                .targetPreview(targetPreview)
                .targetMediaUrls(targetMediaUrls)
                .reporterId(report.getReporter() == null ? null : report.getReporter().getId())
                .reporterName(report.getReporter() == null ? null : report.getReporter().getFullName())
                .reporterEmail(report.getReporter() == null ? null : report.getReporter().getEmail())
                .reason(report.getReason() == null ? null : report.getReason().name())
                .description(report.getDescription())
                .status(report.getStatus() == null ? null : report.getStatus().name())
                .resolvedById(report.getResolvedBy() == null ? null : report.getResolvedBy().getId())
                .resolvedByName(report.getResolvedBy() == null ? null : report.getResolvedBy().getFullName())
                .resolutionNote(report.getResolutionNote())
                .resolvedAt(report.getResolvedAt())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }

    private AdminSocialCommentResponse toCommentResponse(PostComment comment) {
        return AdminSocialCommentResponse.from(
                comment,
                commentReactionRepository.countByComment_Id(comment.getId()),
                postCommentRepository.countByParentCommentIdAndStatus(comment.getId(), CommentStatus.VISIBLE)
        );
    }

    private Post getPostOrThrow(Long postId) {
        validatePositiveId(postId, "Id bài viết");
        return postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết"));
    }

    private PostComment getCommentOrThrow(Long commentId) {
        validatePositiveId(commentId, "Id bình luận");
        return postCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bình luận"));
    }

    private SocialReport getReportOrThrow(Long reportId) {
        validatePositiveId(reportId, "Id báo cáo");
        return socialReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy báo cáo"));
    }

    private SocialReport getPendingReportOrThrow(Long reportId) {
        SocialReport report = getReportOrThrow(reportId);
        if (report.getStatus() != SocialReport.ReportStatus.pending
                && report.getStatus() != SocialReport.ReportStatus.reviewing) {
            throw new BadRequestException("Báo cáo này đã được xử lý");
        }
        return report;
    }

    private User getModerator(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new BadRequestException("Không xác định được người kiểm duyệt");
        }
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người kiểm duyệt"));
    }

    private void recordAction(
            SocialReport.ModerationTargetType targetType,
            Long targetId,
            ModerationAction.ModerationActionType actionType,
            String reason,
            User moderator,
            SocialReport report
    ) {
        ModerationAction action = new ModerationAction();
        action.setTargetType(targetType);
        action.setTargetId(targetId);
        action.setAction(actionType);
        action.setReason(trimToNull(reason));
        action.setModerator(moderator);
        action.setReport(report);
        moderationActionRepository.save(action);
    }

    private String reasonFrom(ModerationActionRequest request) {
        return request == null ? null : trimToNull(request.getReason());
    }

    private void addCreatedRange(
            List<Predicate> predicates,
            jakarta.persistence.criteria.Path<LocalDateTime> path,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            LocalDateTime from,
            LocalDateTime to
    ) {
        if (from != null) {
            predicates.add(cb.greaterThanOrEqualTo(path, from));
        }
        if (to != null) {
            predicates.add(cb.lessThanOrEqualTo(path, to));
        }
    }

    private void validateTimeRange(LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("Thời gian bắt đầu không được sau thời gian kết thúc");
        }
    }

    private void validatePositiveId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new BadRequestException(fieldName + " phải lớn hơn 0");
        }
    }

    private int validatePage(int page) {
        if (page < 0) {
            throw new BadRequestException("Số trang không được âm");
        }
        return page;
    }

    private int validateSize(int size) {
        if (size <= 0) {
            throw new BadRequestException("Kích thước trang phải lớn hơn 0");
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
