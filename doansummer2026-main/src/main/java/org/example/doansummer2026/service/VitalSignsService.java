package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.dto.vitalSigns.VitalSignsCreateRequest;
import org.example.doansummer2026.dto.vitalSigns.VitalSignsResponse;
import org.example.doansummer2026.dto.vitalSigns.VitalSignsUpdateRequest;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.MedicalRecord;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.model.VitalSigns;
import org.example.doansummer2026.repository.MedicalRecordRepository;
import org.example.doansummer2026.repository.StaffInfoRepository;
import org.example.doansummer2026.repository.VitalSignsRepository;
import org.springframework.stereotype.Service;
import org.example.doansummer2026.service.interfaces.VitalSignsServiceInterface;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class VitalSignsService implements VitalSignsServiceInterface {

    private final VitalSignsRepository repo;
    private final MedicalRecordRepository medicalRecordRepo;
    private final StaffInfoRepository staffRepo;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public VitalSignsResponse get(UUID id) {
        return VitalSignsResponse.from(findById(id));
    }

    public VitalSignsResponse create(VitalSignsCreateRequest req) {
        MedicalRecord record = medicalRecordRepo.findById(req.medicalRecordId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Hồ sơ bệnh án không tồn tại: " + req.medicalRecordId()));
        if (repo.findByMedicalRecord_RecordId(req.medicalRecordId()).isPresent()) {
            throw new ConflictException("Hồ sơ đã có chỉ số sinh hiệu; vui lòng dùng chức năng cập nhật");
        }
        ensureRecordEditable(record);
        StaffInfo recordedBy = resolveCurrentRecorder(req.recordedById());
        VitalSigns v = VitalSigns.builder()
                .medicalRecord(record)
                .bloodPressure(req.bloodPressure())
                .heartRate(req.heartRate())
                .temperature(req.temperature())
                .weight(req.weight())
                .height(req.height())
                .recordedAt(LocalDateTime.now())
                .recordedBy(recordedBy)
                .build();
        VitalSigns saved = repo.save(v);
        record.setVitalSigns(saved);
        medicalRecordRepo.save(record);
        return VitalSignsResponse.from(saved);
    }

    public VitalSignsResponse update(UUID id, VitalSignsUpdateRequest req) {
        VitalSigns v = findById(id);
        ensureRecordEditable(v.getMedicalRecord());
        if (req.bloodPressure() != null) v.setBloodPressure(req.bloodPressure());
        if (req.heartRate() != null) v.setHeartRate(req.heartRate());
        if (req.temperature() != null) v.setTemperature(req.temperature());
        if (req.weight() != null) v.setWeight(req.weight());
        if (req.height() != null) v.setHeight(req.height());
        return VitalSignsResponse.from(repo.save(v));
    }

    public void delete(UUID id) {
        findById(id);
        throw new ConflictException(
                "Không xóa chỉ số sinh hiệu để tránh mất lịch sử; vui lòng cập nhật lại số liệu nếu nhập sai");
    }

    public VitalSigns findById(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chỉ số sinh hiệu không tồn tại: " + id));
    }

    private StaffInfo resolveCurrentRecorder(UUID requestedRecorderId) {
        if (authService.getCurrentSystemRole() == org.example.doansummer2026.enums.SystemRole.ADMIN) {
            if (requestedRecorderId == null) return null;
            return staffRepo.findById(requestedRecorderId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Nhân viên không tồn tại: " + requestedRecorderId));
        }
        UUID staffId = authService.currentStaffId();
        if (staffId == null) {
            throw new BadRequestException("Không xác định được nhân viên đang ghi chỉ số sinh hiệu");
        }
        return staffRepo.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Nhân viên không tồn tại: " + staffId));
    }

    private void ensureRecordEditable(MedicalRecord record) {
        if (record == null) {
            throw new BadRequestException("Chỉ số sinh hiệu chưa gắn với hồ sơ khám");
        }
        if (record.getStatus() == org.example.doansummer2026.enums.MedicalRecordStatus.COMPLETED) {
            throw new ConflictException("Không thể sửa chỉ số sinh hiệu của hồ sơ đã hoàn thành");
        }
        if (authService.getCurrentSystemRole() == org.example.doansummer2026.enums.SystemRole.ADMIN) return;

        UUID staffId = authService.currentStaffId();
        if (staffId == null) {
            throw new BadRequestException("Không xác định được nhân viên đang thao tác");
        }
        if (record.getQueueTicket() != null && record.getQueueTicket().getDepartment() != null) {
            var department = record.getQueueTicket().getDepartment();
            boolean headDoctor = department.getHeadDoctor() != null
                    && staffId.equals(department.getHeadDoctor().getStaffId());
            boolean assignedNurse = department.getNurses() != null
                    && department.getNurses().stream()
                    .anyMatch(nurse -> staffId.equals(nurse.getStaffId()));
            if (!headDoctor && !assignedNurse) {
                throw new BadRequestException("Bạn không được phân công cập nhật hồ sơ tại phòng này");
            }
            return;
        }
        if (record.getDoctor() == null || !staffId.equals(record.getDoctor().getStaffId())) {
            throw new BadRequestException("Bạn không phải bác sĩ phụ trách hồ sơ này");
        }
    }
}




