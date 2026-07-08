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
import org.example.doansummer2026.model.CustomerVisit;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.repository.MedicalRecordRepository;
import org.example.doansummer2026.repository.CustomerVisitRepository;
import org.example.doansummer2026.repository.StaffInfoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.example.doansummer2026.service.interfaces.MedicalRecordServiceInterface;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class MedicalRecordService implements MedicalRecordServiceInterface {

    private final MedicalRecordRepository repo;
    private final CustomerVisitRepository visitRepo;
    private final StaffInfoRepository staffRepo;

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
        return MedicalRecordResponse.from(repo.save(r), false);
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
        return MedicalRecordResponse.from(repo.save(r), false);
    }

    public MedicalRecordResponse complete(UUID id) {
        MedicalRecord r = findById(id);
        if (r.getStatus() == MedicalRecordStatus.COMPLETED) {
            throw new BadRequestException("Ho so da duoc dong truoc do");
        }
        r.setStatus(MedicalRecordStatus.COMPLETED);
        r.setCompletedAt(LocalDateTime.now());
        return MedicalRecordResponse.from(repo.save(r), true);
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