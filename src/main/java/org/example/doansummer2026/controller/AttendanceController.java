package org.example.doansummer2026.controller;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.RestResponses;
import org.example.doansummer2026.dto.attendance.*;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;

@RestController @RequestMapping("/api/v1/attendance") @RequiredArgsConstructor
public class AttendanceController {
 private final AttendanceService service; private final AuthService authService;
 @GetMapping("/kiosk-token") @PreAuthorize("hasRole('CLINIC_MANAGER')")
 public ResponseEntity<QrTokenResponse> token(){return RestResponses.ok(service.issueToken(staffId()));}
 @PostMapping("/scan") @PreAuthorize("hasRole('STAFF')")
 public ResponseEntity<AttendanceResponse> scan(@RequestBody Map<String,String> body,HttpServletRequest req){String f=req.getHeader("X-Forwarded-For");String ip=f==null||f.isBlank()?req.getRemoteAddr():f.split(",")[0].trim();return RestResponses.ok(service.scan(staffId(),body.get("token"),ip,req.getHeader("User-Agent")));}
 @GetMapping("/me/today") @PreAuthorize("hasRole('STAFF')")
 public ResponseEntity<List<AttendanceTodayResponse>> today(){return RestResponses.ok(service.today(staffId()));}
 @PostMapping("/adjustments") @PreAuthorize("hasRole('STAFF')")
 public ResponseEntity<AdjustmentResponse> request(@Valid @RequestBody AdjustmentRequest req){return RestResponses.ok(service.request(staffId(),req));}
 @GetMapping("/manage") @PreAuthorize("hasRole('CLINIC_MANAGER')")
 public ResponseEntity<List<AttendanceManagementResponse>> manage(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date){return RestResponses.ok(service.manage(date==null?LocalDate.now():date));}
 @GetMapping("/adjustments/pending") @PreAuthorize("hasRole('CLINIC_MANAGER')")
 public ResponseEntity<List<AdjustmentResponse>> pending(){return RestResponses.ok(service.pending());}
 @PutMapping("/adjustments/{id}/review") @PreAuthorize("hasRole('CLINIC_MANAGER')")
 public ResponseEntity<AdjustmentResponse> review(@PathVariable UUID id,@RequestBody Map<String,Object> body){return RestResponses.ok(service.review(id,staffId(),Boolean.TRUE.equals(body.get("approved")),body.get("note")==null?null:body.get("note").toString()));}
 private UUID staffId(){UUID id=authService.currentStaffId();if(id==null)throw new BadRequestException("Khong tim thay thong tin nhan vien dang dang nhap");return id;}
}
