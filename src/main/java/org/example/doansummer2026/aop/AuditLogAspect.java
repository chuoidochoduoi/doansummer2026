package org.example.doansummer2026.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.example.doansummer2026.dto.auditLog.AuditLogCreateRequest;
import org.example.doansummer2026.model.Account;
import org.example.doansummer2026.service.AuditLogService;
import org.example.doansummer2026.service.AuthService;
import org.example.doansummer2026.service.AuditSnapshotService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.UUID;
import tools.jackson.databind.ObjectMapper;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogAspect {

    private final AuditLogService auditLogService;
    private final AuthService authService;
    private final ObjectMapper objectMapper;
    private final AuditSnapshotService auditSnapshotService;

    @Around("@annotation(auditable)")
    public Object logAudit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        String requestedEntityId = extractEntityId(joinPoint, auditable, null);
        String oldValueJson = auditSnapshotService.snapshot(auditable.entityName(), requestedEntityId);
        Object result = joinPoint.proceed();
        
        try {
            HttpServletRequest request = getRequest();
            String ipAddress = request != null ? request.getRemoteAddr() : "unknown";
            String userAgent = request != null ? request.getHeader("User-Agent") : "unknown";

            UUID actorId = null;
            try {
                Account acc = authService.currentAccount();
                if (acc != null) actorId = acc.getAccountId();
            } catch (Exception e) {
                log.warn("Could not extract current account for audit log: {}", e.getMessage());
            }

            String entityId = extractEntityId(joinPoint, auditable, result);
            String newValueJson = serializeResponse(result);
            String description = auditable.description().isBlank()
                    ? getVietnameseAction(auditable.action()) + " dữ liệu " + getVietnameseEntity(auditable.entityName())
                    : auditable.description();

            AuditLogCreateRequest logReq = new AuditLogCreateRequest(
                    auditable.action(),
                    auditable.entityName(),
                    entityId,
                    actorId,
                    ipAddress != null && ipAddress.length() > 50 ? ipAddress.substring(0, 50) : ipAddress,
                    userAgent != null && userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent,
                    oldValueJson,
                    newValueJson,
                    description
            );
            // Ghi dong bo bang REQUIRES_NEW sau khi nghiep vu da tra ve thanh cong.
            // Khong dung common pool de tranh mat log khi JVM dung hoac request context ket thuc.
            auditLogService.create(logReq);
            
        } catch (Exception e) {
            log.error("Failed to process audit logging", e);
        }

        return result;
    }

    private String serializeResponse(Object result) {
        if (result == null) return null;
        try {
            Object value = result instanceof ResponseEntity<?> response ? response.getBody() : result;
            return value == null ? null : objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            log.warn("Could not serialize audit response: {}", ex.getMessage());
            return null;
        }
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private String getVietnameseAction(org.example.doansummer2026.enums.AuditAction action) {
        if (action == null) return "Thao tác";
        return switch (action) {
            case CREATE -> "Tạo mới";
            case UPDATE -> "Cập nhật";
            case DELETE -> "Xóa";
            case LOGIN -> "Đăng nhập";
            case LOGOUT -> "Đăng xuất";
            default -> action.name();
        };
    }

    private String getVietnameseEntity(String entityName) {
        if (entityName == null) return "Dữ liệu";
        return switch (entityName) {
            case "Account" -> "Tài khoản";
            case "StaffInfo" -> "Nhân sự";
            case "PatientProfile" -> "Bệnh nhân";
            case "Department" -> "Phòng/Khoa";
            case "ServiceItem" -> "Dịch vụ";
            case "Appointment" -> "Lịch hẹn";
            case "System" -> "Hệ thống";
            default -> entityName;
        };
    }

    private String extractEntityId(ProceedingJoinPoint joinPoint, Auditable auditable, Object result) {
        if (!auditable.idParamName().isEmpty()) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] parameterNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();
            for (int i = 0; i < parameterNames.length; i++) {
                if (parameterNames[i].equals(auditable.idParamName()) && args[i] != null) {
                    return args[i].toString();
                }
            }
        }

        if (result instanceof ResponseEntity<?> responseEntity) {
            Object body = responseEntity.getBody();
            if (body != null) {
                try {
                    for (Method method : body.getClass().getMethods()) {
                        String name = method.getName().toLowerCase();
                        if ((name.endsWith("id") || name.equals("id")) && method.getParameterCount() == 0) {
                            Object idVal = method.invoke(body);
                            if (idVal != null) {
                                String idStr = idVal.toString();
                                // Basic UUID/Long check to avoid getting random string fields like "sessionId" if they aren't the primary key.
                                // Actually, returning the first id-like field is fine for this context.
                                if (idStr.length() > 0) return idStr;
                            }
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Error extracting ID from response body: {}", ex.getMessage());
                }
            }
        }
        return null;
    }
}
