package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.dto.vitalSigns.VitalSignsCreateRequest;
import org.example.doansummer2026.dto.vitalSigns.VitalSignsResponse;
import org.example.doansummer2026.dto.vitalSigns.VitalSignsUpdateRequest;
import org.example.doansummer2026.exception.ConflictException;
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

    @Transactional(readOnly = true)
    public VitalSignsResponse get(UUID id) {
        return VitalSignsResponse.from(findById(id));
    }

    public VitalSignsResponse create(VitalSignsCreateRequest req) {
        MedicalRecord record = medicalRecordRepo.findById(req.medicalRecordId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ho so benh an khong ton tai: " + req.medicalRecordId()));
        if (repo.findByMedicalRecord_RecordId(req.medicalRecordId()).isPresent()) {
            throw new ConflictException("Ho so da co chi so sinh hieu; dung PUT de cap nhat");
        }
        StaffInfo recordedBy = null;
        if (req.recordedById() != null) {
            recordedBy = staffRepo.findById(req.recordedById())
                    .orElseThrow(() -> new ResourceNotFoundException("Nhan vien khong ton tai: " + req.recordedById()));
        }
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
        if (req.bloodPressure() != null) v.setBloodPressure(req.bloodPressure());
        if (req.heartRate() != null) v.setHeartRate(req.heartRate());
        if (req.temperature() != null) v.setTemperature(req.temperature());
        if (req.weight() != null) v.setWeight(req.weight());
        if (req.height() != null) v.setHeight(req.height());
        return VitalSignsResponse.from(repo.save(v));
    }

    public void delete(UUID id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Chi so khong ton tai: " + id);
        }
        repo.deleteById(id);
    }

    public VitalSigns findById(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chi so khong ton tai: " + id));
    }
}
