package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.dto.shift.ShiftConfigCreateRequest;
import org.example.doansummer2026.dto.shift.ShiftConfigResponse;
import org.example.doansummer2026.dto.shift.ShiftConfigUpdateRequest;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.ShiftConfig;
import org.example.doansummer2026.repository.ShiftConfigRepository;
import org.example.doansummer2026.repository.AppointmentRepository;
import org.example.doansummer2026.repository.StaffScheduleRepository;
import org.example.doansummer2026.repository.StaffScheduleTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ShiftConfigService {

    private static final java.time.ZoneId CLINIC_ZONE = java.time.ZoneId.of("Asia/Ho_Chi_Minh");

    private final ShiftConfigRepository shiftConfigRepository;
    private final AppointmentRepository appointmentRepository;
    private final StaffScheduleRepository staffScheduleRepository;
    private final StaffScheduleTemplateRepository staffScheduleTemplateRepository;

    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void initDefaultShifts() {
        if (shiftConfigRepository.count() == 0) {
            shiftConfigRepository.save(ShiftConfig.builder()
                    .name("Ca Sáng")
                    .startTime("07:30")
                    .endTime("11:30")
                    .isActive(true)
                    .build());
            shiftConfigRepository.save(ShiftConfig.builder()
                    .name("Ca Chiều")
                    .startTime("13:30")
                    .endTime("17:30")
                    .isActive(true)
                    .build());
        }
    }

    @Transactional(readOnly = true)
    public List<ShiftConfigResponse> getAllActiveShifts() {
        return shiftConfigRepository.findAllByIsActiveTrueOrderByStartTimeAsc()
                .stream()
                .map(ShiftConfigResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ShiftConfigResponse> getAllShifts() {
        return shiftConfigRepository.findAllByOrderByStartTimeAsc()
                .stream()
                .map(ShiftConfigResponse::from)
                .collect(Collectors.toList());
    }

    public ShiftConfigResponse createShift(ShiftConfigCreateRequest request) {
        String name = normalizeName(request.name());
        if (shiftConfigRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Tên ca làm đã tồn tại");
        }
        validateTimeFormat(request.startTime());
        validateTimeFormat(request.endTime());
        validateTimeLogic(request.startTime(), request.endTime());
        boolean active = request.isActive() == null || request.isActive();
        if (active) validateOverlap(request.startTime(), request.endTime(), null);

        ShiftConfig shiftConfig = ShiftConfig.builder()
                .name(name)
                .startTime(request.startTime())
                .endTime(request.endTime())
                .isActive(active)
                .build();
        return ShiftConfigResponse.from(shiftConfigRepository.save(shiftConfig));
    }

    public ShiftConfigResponse updateShift(UUID shiftId, ShiftConfigUpdateRequest request) {
        ShiftConfig shiftConfig = shiftConfigRepository.findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca khám"));

        boolean changesDefinition = request.name() != null
                || request.startTime() != null || request.endTime() != null;
        if (changesDefinition && staffScheduleRepository.countByShift_ShiftId(shiftId) > 0) {
            throw new ConflictException(
                    "Không thể sửa tên hoặc khung giờ của ca đã phát sinh lịch trực. "
                            + "Hãy tạo ca mới và ngừng ca cũ để giữ đúng lịch sử");
        }

        String newStart = request.startTime() != null ? request.startTime() : shiftConfig.getStartTime();
        String newEnd = request.endTime() != null ? request.endTime() : shiftConfig.getEndTime();
        boolean targetActive = request.isActive() != null ? request.isActive() : shiftConfig.getIsActive();

        if (request.name() != null) {
            String name = normalizeName(request.name());
            if (shiftConfigRepository.existsByNameIgnoreCaseAndShiftIdNot(name, shiftId)) {
                throw new ConflictException("Tên ca làm đã tồn tại");
            }
            shiftConfig.setName(name);
        }

        if (request.startTime() != null || request.endTime() != null) {
            validateTimeFormat(newStart);
            validateTimeFormat(newEnd);
            validateTimeLogic(newStart, newEnd);
        }
        if (targetActive && (request.startTime() != null || request.endTime() != null
                || Boolean.TRUE.equals(request.isActive()))) {
            validateOverlap(newStart, newEnd, shiftId);
        }

        if (request.startTime() != null) shiftConfig.setStartTime(request.startTime());
        if (request.endTime() != null) shiftConfig.setEndTime(request.endTime());
        if (request.isActive() != null) {
            if (!request.isActive()) validateCanDeactivate(shiftId);
            shiftConfig.setIsActive(request.isActive());
        }

        return ShiftConfigResponse.from(shiftConfigRepository.save(shiftConfig));
    }

    public void deleteShift(UUID shiftId) {
        ShiftConfig shift = shiftConfigRepository.findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca khám"));
        long scheduleCount = staffScheduleRepository.countByShift_ShiftId(shiftId);
        long templateCount = staffScheduleTemplateRepository.countByShift_ShiftId(shiftId);
        if (scheduleCount == 0 && templateCount == 0) {
            shiftConfigRepository.delete(shift);
            return;
        }
        validateCanDeactivate(shiftId);
        shift.setIsActive(false);
        shiftConfigRepository.save(shift);
    }

    private void validateCanDeactivate(UUID shiftId) {
        boolean hasFutureSchedule = staffScheduleRepository
                .existsByShift_ShiftIdAndWorkDateGreaterThanEqualAndStatus(
                        shiftId, java.time.LocalDate.now(CLINIC_ZONE), org.example.doansummer2026.enums.ScheduleStatus.SCHEDULED);
        boolean hasActiveTemplate = staffScheduleTemplateRepository
                .existsByShift_ShiftIdAndIsActiveTrue(shiftId);
        if (hasFutureSchedule || hasActiveTemplate) {
            throw new ConflictException(
                    "Không thể ngừng ca làm khi còn lịch trực tương lai hoặc mẫu lịch đang hoạt động. "
                            + "Vui lòng xử lý các lịch liên quan trước."
            );
        }
    }

    private void validateTimeFormat(String time) {
        if (!time.matches("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")) {
            throw new BadRequestException("Định dạng giờ không hợp lệ (HH:mm)");
        }
    }

    private int toMinutes(String time) {
        String[] parts = time.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    private void validateTimeLogic(String startTime, String endTime) {
        int start = toMinutes(startTime);
        int end = toMinutes(endTime);
        if (start >= end) {
            throw new BadRequestException("Thời gian bắt đầu phải trước thời gian kết thúc");
        }
    }

    private void validateOverlap(String startTime, String endTime, UUID excludeId) {
        int start = toMinutes(startTime);
        int end = toMinutes(endTime);
        List<ShiftConfig> allShifts = shiftConfigRepository.findAll();
        for (ShiftConfig shift : allShifts) {
            if (excludeId != null && shift.getShiftId().equals(excludeId)) continue;
            if (!Boolean.TRUE.equals(shift.getIsActive())) continue;
            int sStart = toMinutes(shift.getStartTime());
            int sEnd = toMinutes(shift.getEndTime());
            if (start < sEnd && end > sStart) {
                throw new BadRequestException(String.format("Thời gian ca khám bị trùng lặp với '%s' (%s - %s)",
                        shift.getName(), shift.getStartTime(), shift.getEndTime()));
            }
        }
    }

    private String normalizeName(String name) {
        String normalized = name == null ? "" : name.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            throw new BadRequestException("Tên ca không được để trống");
        }
        return normalized;
    }
}
