package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.dto.shift.ShiftConfigCreateRequest;
import org.example.doansummer2026.dto.shift.ShiftConfigResponse;
import org.example.doansummer2026.dto.shift.ShiftConfigUpdateRequest;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.ShiftConfig;
import org.example.doansummer2026.repository.ShiftConfigRepository;
import org.example.doansummer2026.repository.AppointmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ShiftConfigService {

    private final ShiftConfigRepository shiftConfigRepository;
    private final AppointmentRepository appointmentRepository;

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
        
        try {
            appointmentRepository.migrateMorningShifts();
            appointmentRepository.migrateAfternoonShifts();
        } catch (Exception e) {
            // Ignore if column time_slot does not exist anymore
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
        validateTimeFormat(request.startTime());
        validateTimeFormat(request.endTime());
        validateTimeLogic(request.startTime(), request.endTime());
        validateOverlap(request.startTime(), request.endTime(), null);

        ShiftConfig shiftConfig = ShiftConfig.builder()
                .name(request.name())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .isActive(request.isActive() != null ? request.isActive() : true)
                .build();
        return ShiftConfigResponse.from(shiftConfigRepository.save(shiftConfig));
    }

    public ShiftConfigResponse updateShift(UUID shiftId, ShiftConfigUpdateRequest request) {
        ShiftConfig shiftConfig = shiftConfigRepository.findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca khám"));

        String newStart = request.startTime() != null ? request.startTime() : shiftConfig.getStartTime();
        String newEnd = request.endTime() != null ? request.endTime() : shiftConfig.getEndTime();

        if (request.startTime() != null || request.endTime() != null) {
            validateTimeFormat(newStart);
            validateTimeFormat(newEnd);
            validateTimeLogic(newStart, newEnd);
            validateOverlap(newStart, newEnd, shiftId);
        }

        if (request.name() != null) shiftConfig.setName(request.name());
        if (request.startTime() != null) shiftConfig.setStartTime(request.startTime());
        if (request.endTime() != null) shiftConfig.setEndTime(request.endTime());
        if (request.isActive() != null) shiftConfig.setIsActive(request.isActive());

        return ShiftConfigResponse.from(shiftConfigRepository.save(shiftConfig));
    }

    public void deleteShift(UUID shiftId) {
        if (!shiftConfigRepository.existsById(shiftId)) {
            throw new ResourceNotFoundException("Không tìm thấy ca khám");
        }
        shiftConfigRepository.deleteById(shiftId);
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
            int sStart = toMinutes(shift.getStartTime());
            int sEnd = toMinutes(shift.getEndTime());
            if (start < sEnd && end > sStart) {
                throw new BadRequestException(String.format("Thời gian ca khám bị trùng lặp với '%s' (%s - %s)",
                        shift.getName(), shift.getStartTime(), shift.getEndTime()));
            }
        }
    }
}
