package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.medicalRecord.MedicalRecordCreateRequest;
import org.example.doansummer2026.dto.medicalRecord.MedicalRecordResponse;
import org.example.doansummer2026.dto.medicalRecord.MedicalRecordUpdateRequest;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.MedicalRecord;
import org.example.doansummer2026.enums.MedicalRecordStatus;
import org.example.doansummer2026.enums.QueueStatus;
import org.example.doansummer2026.model.CustomerVisit;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.model.VitalSigns;
import org.example.doansummer2026.repository.MedicalRecordRepository;
import org.example.doansummer2026.repository.CustomerVisitRepository;
import org.example.doansummer2026.repository.StaffInfoRepository;
import org.example.doansummer2026.repository.VitalSignsRepository;
import org.example.doansummer2026.repository.QueueTicketRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.example.doansummer2026.service.interfaces.MedicalRecordServiceInterface;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class MedicalRecordService implements MedicalRecordServiceInterface {

    private final MedicalRecordRepository repo;
    private final CustomerVisitRepository visitRepo;
    private final StaffInfoRepository staffRepo;
    private final VitalSignsRepository vitalRepo;
    private final QueueTicketRepository queueTicketRepo;

    @Transactional(readOnly = true)
    public PageResponse<MedicalRecordResponse> search(UUID doctorId, MedicalRecordStatus status,
                                                       LocalDateTime from, LocalDateTime to,
                                                       Pageable pageable) {
        Page<MedicalRecord> page = repo.search(doctorId, status, from, to, pageable);
        return PageResponse.from(page, r -> MedicalRecordResponse.from(r, false));
    }

    @Transactional(readOnly = true)
    public MedicalRecordResponse get(UUID id) {
        return MedicalRecordResponse.from(findById(id), true);
    }

    public MedicalRecordResponse create(MedicalRecordCreateRequest req) {
        CustomerVisit visit = visitRepo.findById(req.visitId())
                .orElseThrow(() -> new ResourceNotFoundException("Luot kham khong ton tai: " + req.visitId()));
        if (repo.findByVisit_VisitId(req.visitId()).isPresent()) {
            throw new ConflictException("Luot kham da co ho so benh an");
        }
        StaffInfo doctor = staffRepo.findById(req.doctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Bac si khong ton tai: " + req.doctorId()));
        MedicalRecord r = MedicalRecord.builder()
                .visit(visit)
                .doctor(doctor)
                .chiefComplaint(req.chiefComplaint())
                .status(MedicalRecordStatus.IN_PROGRESS)
                .build();

        // Tao vital signs neu co du lieu
        if (hasVitalSigns(req)) {
            StaffInfo recordedBy = staffRepo.findById(req.recordedById())
                    .orElseThrow(() -> new ResourceNotFoundException("Nhan vien khong ton tai: " + req.recordedById()));
            VitalSigns v = VitalSigns.builder()
                    .medicalRecord(r)
                    .bloodPressure(req.bloodPressure())
                    .heartRate(req.heartRate())
                    .temperature(req.temperature())
                    .weight(req.weight())
                    .height(req.height())
                    .recordedBy(recordedBy)
                    .build();
            r.setVitalSigns(v);
        }

        return MedicalRecordResponse.from(repo.save(r), false);
    }

    private boolean hasVitalSigns(MedicalRecordCreateRequest req) {
        return req.bloodPressure() != null || req.heartRate() != null || req.temperature() != null ||
               req.weight() != null || req.height() != null;
    }

    public MedicalRecordResponse update(UUID id, MedicalRecordUpdateRequest req) {
        MedicalRecord r = findById(id);
        if (r.getStatus() == MedicalRecordStatus.COMPLETED) {
            throw new BadRequestException("Ho so da dong, khong the sua");
        }
        if (req.chiefComplaint() != null) r.setChiefComplaint(req.chiefComplaint());
        if (req.clinicalFindings() != null) r.setClinicalFindings(req.clinicalFindings());
        if (req.diagnosis() != null) r.setDiagnosis(req.diagnosis());
        if (req.prescriptionNote() != null) r.setPrescriptionNote(req.prescriptionNote());
        if (req.conclusion() != null) r.setConclusion(req.conclusion());
        if (req.patientInstruction() != null) r.setPatientInstruction(req.patientInstruction());

        // Cap nhat vital signs neu co du lieu
        if (r.getVitalSigns() != null && hasVitalSignsUpdate(req)) {
            VitalSigns v = r.getVitalSigns();
            if (req.bloodPressure() != null) v.setBloodPressure(req.bloodPressure());
            if (req.heartRate() != null) v.setHeartRate(req.heartRate());
            if (req.temperature() != null) v.setTemperature(req.temperature());
            if (req.weight() != null) v.setWeight(req.weight());
            if (req.height() != null) v.setHeight(req.height());
        }

        return MedicalRecordResponse.from(repo.save(r), false);
    }

    private boolean hasVitalSignsUpdate(MedicalRecordUpdateRequest req) {
        return req.bloodPressure() != null || req.heartRate() != null || req.temperature() != null ||
               req.weight() != null || req.height() != null;
    }

    /**
     * Lục nháp - chỉ cập nhật dữ liệu, không đổi status.
     * Dùng khi bác sĩ đang nhập thông tin, chưa kết luận.
     */
    public MedicalRecordResponse saveDraft(UUID id, MedicalRecordUpdateRequest req) {
        MedicalRecord r = findById(id);
        if (r.getStatus() == MedicalRecordStatus.COMPLETED) {
            throw new BadRequestException("Ho so da dong, khong the luu nham");
        }
        if (req.chiefComplaint() != null) r.setChiefComplaint(req.chiefComplaint());
        if (req.clinicalFindings() != null) r.setClinicalFindings(req.clinicalFindings());
        if (req.diagnosis() != null) r.setDiagnosis(req.diagnosis());
        if (req.prescriptionNote() != null) r.setPrescriptionNote(req.prescriptionNote());
        if (req.conclusion() != null) r.setConclusion(req.conclusion());
        if (req.patientInstruction() != null) r.setPatientInstruction(req.patientInstruction());

        // Tao moi vital signs neu chua co va co du lieu
        if (r.getVitalSigns() == null && hasVitalSignsUpdate(req)) {
            VitalSigns v = VitalSigns.builder()
                    .medicalRecord(r)
                    .bloodPressure(req.bloodPressure())
                    .heartRate(req.heartRate())
                    .temperature(req.temperature())
                    .weight(req.weight())
                    .height(req.height())
                    .build();
            r.setVitalSigns(v);
        } else if (r.getVitalSigns() != null && hasVitalSignsUpdate(req)) {
            VitalSigns v = r.getVitalSigns();
            if (req.bloodPressure() != null) v.setBloodPressure(req.bloodPressure());
            if (req.heartRate() != null) v.setHeartRate(req.heartRate());
            if (req.temperature() != null) v.setTemperature(req.temperature());
            if (req.weight() != null) v.setWeight(req.weight());
            if (req.height() != null) v.setHeight(req.height());
        }

        return MedicalRecordResponse.from(repo.save(r), false);
    }

    public MedicalRecordResponse complete(UUID id, MedicalRecordUpdateRequest req) {
        MedicalRecord r = findById(id);
        if (r.getStatus() == MedicalRecordStatus.COMPLETED) {
            throw new BadRequestException("Ho so da duoc dong truoc do");
        }

        // Luu thong tin truoc khi dong (gan nhu saveDraft)
        if (req != null) {
            if (req.chiefComplaint() != null) r.setChiefComplaint(req.chiefComplaint());
            if (req.clinicalFindings() != null) r.setClinicalFindings(req.clinicalFindings());
            if (req.diagnosis() != null) r.setDiagnosis(req.diagnosis());
            if (req.prescriptionNote() != null) r.setPrescriptionNote(req.prescriptionNote());
            if (req.conclusion() != null) r.setConclusion(req.conclusion());
            if (req.patientInstruction() != null) r.setPatientInstruction(req.patientInstruction());

            // Cap nhat vital signs
            if (r.getVitalSigns() != null && hasVitalSignsUpdate(req)) {
                VitalSigns v = r.getVitalSigns();
                if (req.bloodPressure() != null) v.setBloodPressure(req.bloodPressure());
                if (req.heartRate() != null) v.setHeartRate(req.heartRate());
                if (req.temperature() != null) v.setTemperature(req.temperature());
                if (req.weight() != null) v.setWeight(req.weight());
                if (req.height() != null) v.setHeight(req.height());
            }
        }

        r.setStatus(MedicalRecordStatus.COMPLETED);
        r.setCompletedAt(LocalDateTime.now());
        MedicalRecord saved = repo.save(r);

        // Tu dong cap nhat queue ticket sang DONE
        CustomerVisit visit = saved.getVisit();
        if (visit != null) {
            queueTicketRepo.findByVisit_VisitId(visit.getVisitId()).ifPresent(ticket -> {
                if (ticket.getStatus() == QueueStatus.IN_PROGRESS) {
                    ticket.setStatus(QueueStatus.DONE);
                    ticket.setCompletedAt(LocalDateTime.now());
                    queueTicketRepo.save(ticket);
                }
            });
        }

        var fetched = repo.findByVisit_VisitIdWithVitalSigns(visit.getVisitId()).orElse(saved);
        return MedicalRecordResponse.from(fetched, true);
    }

    public MedicalRecordResponse complete(UUID id) {
        return complete(id, null);
    }

    public void delete(UUID id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Ho so khong ton tai: " + id);
        }
        repo.deleteById(id);
    }

    public MedicalRecord findById(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ho so khong ton tai: " + id));
    }
}