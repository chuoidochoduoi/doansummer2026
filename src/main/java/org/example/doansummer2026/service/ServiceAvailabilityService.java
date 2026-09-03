package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.dto.shift.*;
import org.example.doansummer2026.enums.*;
import org.example.doansummer2026.model.*;
import org.example.doansummer2026.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceAvailabilityService {
    private final StaffScheduleRepository scheduleRepository;
    private final StaffCapabilityRepository capabilityRepository;
    private final MedicalServiceRepository medicalServiceRepository;
    private final ShiftConfigRepository shiftRepository;
    private final ShiftScheduleResolver shiftResolver;
    private final DepartmentRepository departmentRepository;

    public record Evaluation(boolean available, ShiftUnavailableReason reason, List<StaffInfo> eligibleStaff) {}

    public Evaluation evaluate(MedicalService service, LocalDate date, ShiftConfig shift,
                               boolean customerBooking) {
        return evaluate(service, date, shift, customerBooking, null);
    }

    public Evaluation evaluate(MedicalService service, LocalDate date, ShiftConfig shift,
                               boolean customerBooking, UUID excludedStaffId) {
        ShiftScheduleResolver.ResolvedShift resolved = shiftResolver.resolve(shift, date);
        if (!resolved.available()) return new Evaluation(false, resolved.unavailableReason(), List.of());
        if (service.getStatus() != ServiceStatus.ACTIVE) {
            return new Evaluation(false, ShiftUnavailableReason.SERVICE_INACTIVE, List.of());
        }
        Department department = service.getDepartment();
        if (department != null && department.getStatus() != DepartmentStatus.AVAILABLE) {
            return new Evaluation(false, ShiftUnavailableReason.DEPARTMENT_UNAVAILABLE, List.of());
        }
        List<Department> eligibleDepartments = resolveEligibleDepartments(service);
        if (service.getRequiredCapability() != null && eligibleDepartments.isEmpty()) {
                return new Evaluation(false, ShiftUnavailableReason.CAPABILITY_UNAVAILABLE, List.of());
        }
        if (service.getDepartmentType() == DepartmentType.EXAMINATION
                && service.getRequiredSpecialization() != null && eligibleDepartments.isEmpty()) {
            return new Evaluation(false, ShiftUnavailableReason.DEPARTMENT_UNAVAILABLE, List.of());
        }

        Set<UUID> eligibleDepartmentIds = eligibleDepartments.stream()
                .map(Department::getDepartmentId).collect(java.util.stream.Collectors.toSet());

        List<StaffInfo> eligible = scheduleRepository
                .findAllByWorkDateAndShift_ShiftIdAndStatus(date, shift.getShiftId(), ScheduleStatus.SCHEDULED)
                .stream().map(StaffSchedule::getStaff)
                .filter(staff -> excludedStaffId == null || !excludedStaffId.equals(staff.getStaffId()))
                .filter(this::isActiveStaff)
                .filter(staff -> eligibleDepartmentIds.isEmpty() || (staff.getDepartment() != null
                        && eligibleDepartmentIds.contains(staff.getDepartment().getDepartmentId())))
                .filter(staff -> eligibleForService(staff, service, date))
                .distinct().toList();
        return eligible.isEmpty()
                ? new Evaluation(false, ShiftUnavailableReason.NO_QUALIFIED_STAFF, List.of())
                : new Evaluation(true, null, eligible);
    }

    /**
     * Dịch vụ được định tuyến theo chuyên khoa/năng lực nên không bắt buộc phải
     * lưu cứng một department_id. Cách phân giải này đồng nhất kiểm tra lịch đặt
     * với cách TestRequestService chọn phòng thực hiện sau thanh toán.
     */
    private List<Department> resolveEligibleDepartments(MedicalService service) {
        if (service.getDepartment() != null) {
            Department configured = service.getDepartment();
            if (service.getRequiredCapability() != null && configured.getCapabilities().stream().noneMatch(capability ->
                    capability.getCapabilityId().equals(service.getRequiredCapability().getCapabilityId())
                            && Boolean.TRUE.equals(capability.getActive()))) {
                return List.of();
            }
            if (service.getRequiredSpecialization() != null && (configured.getSpecialization() == null
                    || !configured.getSpecialization().getSpecializationId()
                    .equals(service.getRequiredSpecialization().getSpecializationId()))) {
                return List.of();
            }
            return List.of(configured);
        }
        if (service.getRequiredCapability() != null) {
            return departmentRepository.findEligibleByCapability(
                    service.getRequiredCapability().getCapabilityId()).stream()
                    .filter(department -> department.getStatus() == DepartmentStatus.AVAILABLE)
                    .toList();
        }
        if (service.getDepartmentType() == DepartmentType.EXAMINATION
                && service.getRequiredSpecialization() != null) {
            return departmentRepository.findEligibleExaminationRoomsBySpecialization(
                    service.getRequiredSpecialization().getSpecializationId()).stream()
                    .filter(department -> department.getStatus() == DepartmentStatus.AVAILABLE)
                    .toList();
        }
        return List.of();
    }

    public ServiceCoverageResponse coverage(LocalDate date, UUID shiftId) {
        ShiftConfig shift = shiftRepository.findById(shiftId).orElse(null);
        List<ServiceCoverageItemResponse> items = new ArrayList<>();
        for (MedicalService service : medicalServiceRepository.findAllByStatus(ServiceStatus.ACTIVE)) {
            Evaluation evaluation = shift == null
                    ? new Evaluation(false, ShiftUnavailableReason.SHIFT_OFF, List.of())
                    : evaluate(service, date, shift, false);
            items.add(new ServiceCoverageItemResponse(service.getServiceId(), service.getServiceCode(),
                    service.getName(), evaluation.available(), evaluation.reason(),
                    evaluation.eligibleStaff().stream().map(StaffInfo::getStaffId).toList(),
                    evaluation.eligibleStaff().stream().map(this::staffName).toList()));
        }
        int covered = (int) items.stream().filter(ServiceCoverageItemResponse::available).count();
        return new ServiceCoverageResponse(date, shiftId, items.size(), covered, items.size() - covered, items);
    }

    private boolean eligibleForService(StaffInfo staff, MedicalService service, LocalDate date) {
        if (service.getDepartmentType() == DepartmentType.EXAMINATION) {
            if (staff.getSystemRole() == null || !staff.getSystemRole().isDoctor()) return false;
            if (service.getRequiredSpecialization() != null) {
                return staff.getSpecialization() != null && staff.getSpecialization().getSpecializationId()
                        .equals(service.getRequiredSpecialization().getSpecializationId());
            }
            return true;
        }
        if (service.getRequiredCapability() == null) return true;
        return capabilityRepository.findAllByStaff_StaffIdAndStatus(staff.getStaffId(), StaffCapabilityStatus.ACTIVE)
                .stream().anyMatch(capability -> capability.getCapability().getCapabilityId()
                        .equals(service.getRequiredCapability().getCapabilityId())
                        && (capability.getExpiryDate() == null || !capability.getExpiryDate().isBefore(date)));
    }

    private boolean isActiveStaff(StaffInfo staff) {
        return staff != null && staff.getProfile() != null && staff.getProfile().getAccount() != null
                && Boolean.TRUE.equals(staff.getProfile().getAccount().getIsActive());
    }

    private String staffName(StaffInfo staff) {
        String name = staff.getProfile() == null ? null : staff.getProfile().getFullName();
        return name == null || name.isBlank() ? staff.getStaffCode() : name;
    }
}
