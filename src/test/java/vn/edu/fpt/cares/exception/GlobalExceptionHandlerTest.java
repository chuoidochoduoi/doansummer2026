package vn.edu.fpt.cares.exception;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {
    private ObjectMapper mapper;
    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        handler = new GlobalExceptionHandler(mapper);
        request = new MockHttpServletRequest("POST", "/api/demo");
        response = new MockHttpServletResponse();
    }

    @Test
    void validationReturnsFirstFieldMessageAndAllErrors() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult binding = mock(BindingResult.class);
        when(exception.getBindingResult()).thenReturn(binding);
        when(binding.getFieldErrors()).thenReturn(List.of(
                new FieldError("request", "name", "Tên không hợp lệ"),
                new FieldError("request", "phone", "Số điện thoại không hợp lệ")));

        var result = handler.handleValidation(exception, request, response);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        String json = text(result.getBody());
        assertTrue(json.contains("Tên không hợp lệ"));
        assertTrue(json.contains("Số điện thoại không hợp lệ"));
        assertTrue(json.contains("/api/demo"));
        assertTrue(response.getContentType().startsWith("application/json"));
        assertEquals("UTF-8", response.getCharacterEncoding());
    }

    @Test
    void validationWithoutFieldErrorsUsesGenericMessage() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult binding = mock(BindingResult.class);
        when(exception.getBindingResult()).thenReturn(binding);
        when(binding.getFieldErrors()).thenReturn(List.of());
        assertTrue(text(handler.handleValidation(exception, request, response).getBody())
                .contains("Dữ liệu không hợp lệ"));
    }

    @Test
    void domainSecurityAndUnexpectedExceptionsMapToStableStatuses() {
        assertResponse(handler.handleNotFound(new ResourceNotFoundException("Không thấy"), request, response),
                404, "Không thấy");
        assertResponse(handler.handleConflict(new ConflictException("Xung đột"), request, response),
                409, "Xung đột");
        assertResponse(handler.handleBadRequest(new BadRequestException("Sai dữ liệu"), request, response),
                400, "Sai dữ liệu");
        assertResponse(handler.handleServiceUnavailable(new ServiceUnavailableException("BHXH lỗi"), request, response),
                503, "BHXH lỗi");
        assertResponse(handler.handleAccessDenied(new AccessDeniedException("secret"), request, response),
                403, "Không có quyền truy cập");
        assertResponse(handler.handleAuth(new BadCredentialsException("secret"), request, response),
                401, "Cần xác thực");
        assertResponse(handler.handleAll(new IllegalStateException("secret"), request, response),
                500, "Lỗi hệ thống");
    }

    @Test
    void dataIntegrityDistinguishesDuplicateForeignKeyAndOtherConstraint() {
        assertResponse(handler.handleDataIntegrity(
                        new DataIntegrityViolationException("x", new RuntimeException("duplicate key value")),
                        request, response), 409, "Dữ liệu đã tồn tại");
        assertResponse(handler.handleDataIntegrity(
                        new DataIntegrityViolationException("x", new RuntimeException("FOREIGN KEY violation")),
                        request, response), 409, "dữ liệu đang được sử dụng");
        assertResponse(handler.handleDataIntegrity(
                        new DataIntegrityViolationException("constraint"), request, response),
                409, "Dữ liệu vi phạm ràng buộc");
    }

    @Test
    void constraintViolationUsesFirstMessageOrFallback() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = (ConstraintViolation<Object>) java.lang.reflect.Proxy.newProxyInstance(
                ConstraintViolation.class.getClassLoader(),
                new Class<?>[]{ConstraintViolation.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("getMessage".equals(name)) return "Giá trị vượt giới hạn";
                    if ("hashCode".equals(name)) return 0;
                    if ("equals".equals(name)) return proxy == args[0];
                    if ("toString".equals(name)) return "Proxy";
                    return null;
                });
        assertResponse(handler.handleConstraintViolation(
                        new ConstraintViolationException(Set.of(violation)), request, response),
                400, "Giá trị vượt giới hạn");
        assertResponse(handler.handleConstraintViolation(
                        new ConstraintViolationException(Set.of()), request, response),
                400, "Dữ liệu không hợp lệ");
    }

    @Test
    void committedResponseIsNotReset() {
        HttpServletResponse committed = mock(HttpServletResponse.class);
        when(committed.isCommitted()).thenReturn(true);
        handler.handleBadRequest(new BadRequestException("Sai"), request, committed);
        verify(committed, never()).resetBuffer();
        verify(committed, never()).setContentType(anyString());
    }

    @Test
    void serializationFailureReturnsSafeFallbackWithoutOriginalMessage() throws Exception {
        ObjectMapper failing = mock(ObjectMapper.class);
        when(failing.writeValueAsString(any())).thenThrow(new IllegalStateException("serializer details"));
        GlobalExceptionHandler fallbackHandler = new GlobalExceptionHandler(failing);
        var result = fallbackHandler.handleAll(new IllegalStateException("sensitive"), request, response);
        String body = text(result.getBody());
        assertTrue(body.contains("Lỗi hệ thống"));
        assertFalse(body.contains("sensitive"));
        assertFalse(body.contains("serializer details"));
    }

    private void assertResponse(org.springframework.http.ResponseEntity<byte[]> result,
                                int status, String messagePart) {
        assertEquals(status, result.getStatusCode().value());
        assertTrue(text(result.getBody()).contains(messagePart));
        assertEquals("application/json", result.getHeaders().getContentType().toString());
    }

    private String text(byte[] body) {
        return new String(body, StandardCharsets.UTF_8);
    }
}
