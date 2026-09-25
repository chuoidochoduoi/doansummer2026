package vn.edu.fpt.cares.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.edu.fpt.cares.common.ApiError;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.List;
import tools.jackson.databind.ObjectMapper;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ObjectMapper objectMapper;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<byte[]> handleValidation(MethodArgumentNotValidException ex,
                                                     HttpServletRequest req,
                                                     HttpServletResponse response) {
        List<ApiError.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        String message = errors.isEmpty() ? "Dữ liệu không hợp lệ" : errors.get(0).message();
        ApiError body = new ApiError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Yêu cầu không hợp lệ",
                message,
                req.getRequestURI(),
                errors);
        prepareJsonResponse(response);
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(toJsonBytes(body));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<byte[]> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req,
                                                   HttpServletResponse response) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, response, null);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<byte[]> handleConflict(ConflictException ex, HttpServletRequest req,
                                                   HttpServletResponse response) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req, response, null);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<byte[]> handleBadRequest(BadRequestException ex, HttpServletRequest req,
                                                     HttpServletResponse response) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, response, null);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<byte[]> handleServiceUnavailable(ServiceUnavailableException ex,
                                                             HttpServletRequest req,
                                                             HttpServletResponse response) {
        log.warn("External service unavailable: {}", ex.getMessage());
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), req, response, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<byte[]> handleDataIntegrity(DataIntegrityViolationException ex,
                                                       HttpServletRequest req,
                                                       HttpServletResponse response) {
        log.warn("Data integrity violation", ex);
        String cause = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : null;
        String msg;
        if (cause != null && cause.toLowerCase().contains("duplicate key")) {
            msg = "Dữ liệu đã tồn tại, vui lòng kiểm tra lại thông tin vừa nhập";
        } else if (cause != null && cause.toLowerCase().contains("foreign key")) {
            msg = "Không thể thực hiện vì dữ liệu đang được sử dụng ở nơi khác";
        } else {
            msg = "Dữ liệu vi phạm ràng buộc, vui lòng kiểm tra lại";
        }
        return build(HttpStatus.CONFLICT, msg, req, response, null);
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<byte[]> handleConstraintViolation(jakarta.validation.ConstraintViolationException ex,
                                                              HttpServletRequest req,
                                                              HttpServletResponse response) {
        log.warn("Constraint violation", ex);
        String msg = ex.getConstraintViolations().stream()
                .map(v -> v.getMessage())
                .findFirst()
                .orElse("Dữ liệu không hợp lệ");
        return build(HttpStatus.BAD_REQUEST, msg, req, response, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<byte[]> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req,
                                                       HttpServletResponse response) {
        return build(HttpStatus.FORBIDDEN, "Không có quyền truy cập", req, response, null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<byte[]> handleAuth(AuthenticationException ex, HttpServletRequest req,
                                               HttpServletResponse response) {
        return build(HttpStatus.UNAUTHORIZED, "Cần xác thực", req, response, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<byte[]> handleAll(Exception ex, HttpServletRequest req,
                                              HttpServletResponse response) {
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống", req, response, null);
    }

    private ApiError.FieldError toFieldError(FieldError fe) {
        return new ApiError.FieldError(fe.getField(), fe.getDefaultMessage());
    }

    private ResponseEntity<byte[]> build(HttpStatus status, String message,
                                           HttpServletRequest req,
                                           HttpServletResponse response,
                                           List<ApiError.FieldError> errors) {
        ApiError body = new ApiError(
                Instant.now(),
                status.value(),
                vietnameseErrorLabel(status),
                message,
                req.getRequestURI(),
                errors);
        prepareJsonResponse(response);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(toJsonBytes(body));
    }

    private void prepareJsonResponse(HttpServletResponse response) {
        if (!response.isCommitted()) {
            response.resetBuffer();
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
        }
    }

    private String vietnameseErrorLabel(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Yêu cầu không hợp lệ";
            case UNAUTHORIZED -> "Chưa xác thực";
            case FORBIDDEN -> "Không có quyền truy cập";
            case NOT_FOUND -> "Không tìm thấy dữ liệu";
            case CONFLICT -> "Xung đột dữ liệu";
            case SERVICE_UNAVAILABLE -> "Dịch vụ tạm thời không khả dụng";
            case INTERNAL_SERVER_ERROR -> "Lỗi hệ thống";
            default -> "Không thể xử lý yêu cầu";
        };
    }

    private byte[] toJsonBytes(ApiError body) {
        try {
            return objectMapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
        } catch (Exception serializationError) {
            log.error("Cannot serialize API error response", serializationError);
            return "{\"status\":500,\"error\":\"Lỗi hệ thống\",\"message\":\"Lỗi hệ thống\"}"
                    .getBytes(StandardCharsets.UTF_8);
        }
    }
}



