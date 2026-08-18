package com.petcare.backend.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private static ApiResponse<?> body(ResponseEntity<? extends ApiResponse<?>> response) {
        return response.getBody();
    }

    static Stream<Arguments> typeMismatches() {
        return Stream.of(
                Arguments.of("status", ReminderStatusFilter.class, "Trạng thái lịch nhắc không hợp lệ"),
                Arguments.of("status", Blog.BlogStatus.class, "Trạng thái blog không hợp lệ"),
                Arguments.of("category", Blog.BlogCategory.class, "Danh mục blog không hợp lệ"),
                Arguments.of("status", Pet.PetStatus.class, "Trạng thái thú cưng không hợp lệ"),
                Arguments.of("vaccinePlanStatus", Pet.VaccinePlanStatus.class, "Trạng thái kế hoạch tiêm không hợp lệ"),
                Arguments.of("status", PetVaccination.VaccinationStatus.class, "Trạng thái tiêm không hợp lệ"),
                Arguments.of("targetStage", VaccineTemplate.TargetStage.class, "Giai đoạn vaccine không hợp lệ"),
                Arguments.of("category", CareReminder.ReminderCategory.class, "Loại lịch nhắc không hợp lệ"),
                Arguments.of("status", CareReminderLog.ReminderLogStatus.class, "Trạng thái log lịch nhắc không hợp lệ"),
                Arguments.of("stage", VaccinationReminderLog.VaccinationReminderStage.class, "Giai đoạn nhắc tiêm không hợp lệ"),
                Arguments.of("status", VaccinationReminderLog.VaccinationReminderStatus.class, "Trạng thái log nhắc tiêm không hợp lệ"),
                Arguments.of("status", PostStatus.class, "Trạng thái bài viết không hợp lệ"),
                Arguments.of("privacy", PostPrivacy.class, "Quyền riêng tư bài viết không hợp lệ"),
                Arguments.of("status", CommentStatus.class, "Trạng thái bình luận không hợp lệ"),
                Arguments.of("status", SocialReport.ReportStatus.class, "Trạng thái báo cáo không hợp lệ"),
                Arguments.of("targetType", SocialReport.ModerationTargetType.class, "Loại đối tượng báo cáo không hợp lệ"),
                Arguments.of("reason", SocialReport.ReportReason.class, "Lý do báo cáo không hợp lệ")
        );
    }

    private MethodArgumentTypeMismatchException mismatch(String name, Class<?> requiredType) {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn(name);
        when(ex.getRequiredType()).thenAnswer(invocation -> requiredType);
        return ex;
    }

    @ParameterizedTest
    @MethodSource("typeMismatches")
    void handleTypeMismatch_MapsEverySupportedEnum(String name, Class<?> requiredType, String message) {
        ResponseEntity<ApiResponse<Void>> response = handler.handleTypeMismatch(mismatch(name, requiredType));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body(response)).extracting(ApiResponse::getMessage, ApiResponse::isSuccess)
                .containsExactly(message, false);
    }

    @Test
    void handleTypeMismatch_UsesStatusFallbackAndGenericFallback() {
        assertThat(body(handler.handleTypeMismatch(mismatch("status", String.class))).getMessage())
                .isEqualTo("Trạng thái không hợp lệ");
        assertThat(body(handler.handleTypeMismatch(mismatch("page", Integer.class))).getMessage())
                .isEqualTo("Tham số page không hợp lệ");
    }

    @Test
    void handleValidation_MapsOneAndMultipleFieldErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("request", "name", "Tên không hợp lệ"),
                new FieldError("request", "email", "Email không hợp lệ"),
                new FieldError("request", "name", "Tên cuối cùng")
        ));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ApiResponse<Map<String, String>>> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getData()).containsEntry("name", "Tên cuối cùng")
                .containsEntry("email", "Email không hợp lệ");
    }

    @Test
    @SuppressWarnings("unchecked")
    void handleConstraintViolation_MapsViolationsAndEmptySet() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("request.email");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("Email không hợp lệ");

        ResponseEntity<ApiResponse<Map<String, String>>> withError = handler.handleConstraintViolation(
                new ConstraintViolationException(Set.of(violation)));
        ResponseEntity<ApiResponse<Map<String, String>>> empty = handler.handleConstraintViolation(
                new ConstraintViolationException(Set.of()));

        assertThat(withError.getBody().getData()).containsEntry("request.email", "Email không hợp lệ");
        assertThat(empty.getBody().getData()).isEmpty();
    }

    @Test
    void handlers_MapBadRequestAuthNotFoundForbiddenConflictAndGenericErrors() throws Exception {
        assertStatusAndMessage(handler.handleUnreadableMessage(new HttpMessageNotReadableException("bad")), HttpStatus.BAD_REQUEST, "Dữ liệu request không hợp lệ");
        assertStatusAndMessage(handler.handleMissingParameter(new MissingServletRequestParameterException("petId", "Long")), HttpStatus.BAD_REQUEST, "Thiếu tham số bắt buộc: petId");
        assertStatusAndMessage(handler.handleBadRequest(new BadRequestException("bad")), HttpStatus.BAD_REQUEST, "bad");
        assertStatusAndMessage(handler.handleUnauthorized(new BadCredentialsException("bad")), HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không chính xác");
        assertStatusAndMessage(handler.handleUnauthorized(new UsernameNotFoundException("missing")), HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không chính xác");
        assertStatusAndMessage(handler.handleAuthentication(new InsufficientAuthenticationException("auth")), HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập để truy cập tài nguyên này");
        assertStatusAndMessage(handler.handleAccessDenied(new AccessDeniedException("no")), HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập tài nguyên này");
        assertStatusAndMessage(handler.handleNotFound(new ResourceNotFoundException("missing")), HttpStatus.NOT_FOUND, "missing");
        assertStatusAndMessage(handler.handleNoHandlerFound(new NoHandlerFoundException("GET", "/missing", null)), HttpStatus.NOT_FOUND, "Không tìm thấy API được yêu cầu");
        assertStatusAndMessage(handler.handleForbidden(new ForbiddenException("forbidden")), HttpStatus.FORBIDDEN, "forbidden");
        assertStatusAndMessage(handler.handleConflict(new ConflictException("conflict")), HttpStatus.CONFLICT, "conflict");
        assertStatusAndMessage(handler.handleUnexpected(new RuntimeException()), HttpStatus.INTERNAL_SERVER_ERROR, "Có lỗi xảy ra, vui lòng thử lại sau");
    }

    @Test
    void handleDataIntegrityViolation_UsesPhoneSpecificAndFallbackMessages() {
        assertStatusAndMessage(handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("outer", new IllegalStateException("duplicate phone_number"))),
                HttpStatus.BAD_REQUEST, "Số điện thoại đã được sử dụng");
        assertStatusAndMessage(handler.handleDataIntegrityViolation(new DataIntegrityViolationException("duplicate email")),
                HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ");
        assertStatusAndMessage(handler.handleDataIntegrityViolation(new DataIntegrityViolationException(null)),
                HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ");
    }

    private void assertStatusAndMessage(ResponseEntity<? extends ApiResponse<?>> response, HttpStatus status, String message) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(body(response)).isNotNull();
        assertThat(body(response).isSuccess()).isFalse();
        assertThat(body(response).getMessage()).isEqualTo(message);
    }
}
