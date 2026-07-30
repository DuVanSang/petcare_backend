package com.petcare.backend.exception;

import com.petcare.backend.dto.auth.response.EmailVerificationRequiredResponse;
import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.dto.reminder.request.ReminderStatusFilter;
import com.petcare.backend.model.Blog;
import com.petcare.backend.model.CareReminder;
import com.petcare.backend.model.CareReminderLog;
import com.petcare.backend.model.Pet;
import com.petcare.backend.model.PetVaccination;
import com.petcare.backend.model.SocialReport;
import com.petcare.backend.model.VaccinationReminderLog;
import com.petcare.backend.model.VaccineTemplate;
import com.petcare.backend.model.enums.CommentStatus;
import com.petcare.backend.model.enums.PostPrivacy;
import com.petcare.backend.model.enums.PostStatus;
import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(ApiResponse.error("Dữ liệu không hợp lệ", errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> errors.put(
                violation.getPropertyPath().toString(),
                violation.getMessage()
        ));
        return ResponseEntity.badRequest().body(ApiResponse.error("Dữ liệu không hợp lệ", errors));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        if ("status".equals(ex.getName()) && ex.getRequiredType() == ReminderStatusFilter.class) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Trạng thái lịch nhắc không hợp lệ", null));
        }
        if ("status".equals(ex.getName()) && ex.getRequiredType() == Blog.BlogStatus.class) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Trạng thái blog không hợp lệ", null));
        }
        if ("category".equals(ex.getName()) && ex.getRequiredType() == Blog.BlogCategory.class) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Danh mục blog không hợp lệ", null));
        }
        if ("status".equals(ex.getName()) && ex.getRequiredType() == Pet.PetStatus.class) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Trạng thái thú cưng không hợp lệ", null));
        }
        if ("vaccinePlanStatus".equals(ex.getName()) && ex.getRequiredType() == Pet.VaccinePlanStatus.class) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Trạng thái kế hoạch tiêm không hợp lệ", null));
        }
        if ("status".equals(ex.getName()) && ex.getRequiredType() == PetVaccination.VaccinationStatus.class) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Trạng thái tiêm không hợp lệ", null));
        }
        if ("targetStage".equals(ex.getName()) && ex.getRequiredType() == VaccineTemplate.TargetStage.class) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Giai đoạn vaccine không hợp lệ", null));
        }
        if ("category".equals(ex.getName()) && ex.getRequiredType() == CareReminder.ReminderCategory.class) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Loại lịch nhắc không hợp lệ", null));
        }
        if ("status".equals(ex.getName()) && ex.getRequiredType() == CareReminderLog.ReminderLogStatus.class) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Trạng thái log lịch nhắc không hợp lệ", null));
        }
        if ("stage".equals(ex.getName())
                && ex.getRequiredType() == VaccinationReminderLog.VaccinationReminderStage.class) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Giai đoạn nhắc tiêm không hợp lệ", null));
        }
        if ("status".equals(ex.getName())
                && ex.getRequiredType() == VaccinationReminderLog.VaccinationReminderStatus.class) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Trạng thái log nhắc tiêm không hợp lệ", null));
        }
        if ("status".equals(ex.getName()) && ex.getRequiredType() == PostStatus.class) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Trạng thái bài viết không hợp lệ", null));
        }
        if ("privacy".equals(ex.getName()) && ex.getRequiredType() == PostPrivacy.class) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Quyền riêng tư bài viết không hợp lệ", null));
        }
        if ("status".equals(ex.getName()) && ex.getRequiredType() == CommentStatus.class) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Trạng thái bình luận không hợp lệ", null));
        }
        if ("status".equals(ex.getName()) && ex.getRequiredType() == SocialReport.ReportStatus.class) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Trạng thái báo cáo không hợp lệ", null));
        }
        if ("targetType".equals(ex.getName())
                && ex.getRequiredType() == SocialReport.ModerationTargetType.class) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Loại đối tượng báo cáo không hợp lệ", null));
        }
        if ("reason".equals(ex.getName()) && ex.getRequiredType() == SocialReport.ReportReason.class) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Lý do báo cáo không hợp lệ", null));
        }
        if ("status".equals(ex.getName())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Trạng thái tiêm không hợp lệ", null));
        }

        return ResponseEntity.badRequest().body(ApiResponse.error(
                "Tham số " + ex.getName() + " không hợp lệ",
                null
        ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableMessage(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error("Dữ liệu request không hợp lệ", null));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Thiếu tham số bắt buộc: " + ex.getParameterName(), null));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage(), null));
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ApiResponse<EmailVerificationRequiredResponse>> handleEmailNotVerified(
            EmailNotVerifiedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(
                ex.getMessage(),
                new EmailVerificationRequiredResponse(ex.getEmail(), true)
        ));
    }

    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Email hoặc mật khẩu không chính xác", null));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Bạn cần đăng nhập để truy cập tài nguyên này", null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Bạn không có quyền truy cập tài nguyên này", null));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage(), null));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFound(NoHandlerFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Không tìm thấy API được yêu cầu", null));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(ForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(ex.getMessage(), null));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ex.getMessage(), null));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation", ex);
        String message = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();

        if (message != null && message.toLowerCase().contains("phone_number")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Số điện thoại đã được sử dụng", null));
        }

        return ResponseEntity.badRequest().body(ApiResponse.error("Dữ liệu không hợp lệ", null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Có lỗi xảy ra, vui lòng thử lại sau", null));
    }
}
