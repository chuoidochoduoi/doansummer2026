package org.example.doansummer2026.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.ApiResponse;
import org.example.doansummer2026.dto.shift.ShiftConfigCreateRequest;
import org.example.doansummer2026.dto.shift.ShiftConfigResponse;
import org.example.doansummer2026.dto.shift.ShiftConfigUpdateRequest;
import org.example.doansummer2026.service.ShiftConfigService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shifts")
@RequiredArgsConstructor
public class ShiftConfigController {

    private final ShiftConfigService shiftConfigService;

    // Public API cho bệnh nhân/lễ tân xem lịch ca khám
    @GetMapping("/active")
    public ApiResponse<List<ShiftConfigResponse>> getActiveShifts() {
        return ApiResponse.success(shiftConfigService.getAllActiveShifts());
    }

    // Admin APIs
    @GetMapping
    @PreAuthorize("hasRole('CLINIC_MANAGER')")
    public ApiResponse<List<ShiftConfigResponse>> getAllShifts() {
        return ApiResponse.success(shiftConfigService.getAllShifts());
    }

    @PostMapping
    @PreAuthorize("hasRole('CLINIC_MANAGER')")
    public ApiResponse<ShiftConfigResponse> createShift(@RequestBody @Valid ShiftConfigCreateRequest request) {
        return ApiResponse.success(shiftConfigService.createShift(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CLINIC_MANAGER')")
    public ApiResponse<ShiftConfigResponse> updateShift(@PathVariable UUID id, @RequestBody @Valid ShiftConfigUpdateRequest request) {
        return ApiResponse.success(shiftConfigService.updateShift(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CLINIC_MANAGER')")
    public ApiResponse<Void> deleteShift(@PathVariable UUID id) {
        shiftConfigService.deleteShift(id);
        return ApiResponse.success(null);
    }
}
