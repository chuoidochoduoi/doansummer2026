package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.icd.ICD10SelectionCreateRequest;
import org.example.doansummer2026.dto.medicalRecord.MedicalRecordCreateRequest;
import org.example.doansummer2026.dto.medicalRecord.MedicalRecordResponse;
import org.example.doansummer2026.dto.medicalRecord.MedicalRecordUpdateRequest;
import org.example.doansummer2026.dto.medicalRecord.ReceptionistRecordResponse;
import org.example.doansummer2026.dto.medicalRecord.ReceptionistCustomerResponse;
import org.example.doansummer2026.dto.medicalRecord.ReceptionistAllCustomerResponse;
import org.example.doansummer2026.dto.medicalHistory.MedicalHistoryResponse;
import org.example.doansummer2026.enums.BloodType;
import org.example.doansummer2026.enums.Gender;
import org.example.doansummer2026.dto.medicalRecord.PrescriptionItemCreateRequest;
import org.example.doansummer2026.enums.TestRequestStatus;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.MedicalRecord;
import org.example.doansummer2026.enums.MedicalRecordStatus;
import org.example.doansummer2026.enums.QueueStatus;
import org.springframework.data.jpa.domain.Specification;
import org.example.doansummer2026.model.CustomerVisit;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.model.VitalSigns;
import org.example.doansummer2026.model.PrescriptionItem;
import org.example.doansummer2026.model.Icd10Selection;
import org.example.doansummer2026.model.Icd10Code;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.repository.MedicalRecordRepository;
import org.example.doansummer2026.repository.AppointmentRepository;
import org.example.doansummer2026.repository.CustomerVisitRepository;
import org.example.doansummer2026.repository.StaffInfoRepository;
import org.example.doansummer2026.repository.VitalSignsRepository;
import org.example.doansummer2026.repository.QueueTicketRepository;
import org.example.doansummer2026.repository.Icd10CodeRepository;
import org.example.doansummer2026.repository.TestRequestRepository;
import org.example.doansummer2026.repository.ProfileRepository;
import org.example.doansummer2026.repository.InvoiceRepository;
import org.example.doansummer2026.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.example.doansummer2026.service.interfaces.MedicalRecordServiceInterface;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class MedicalRecordService implements MedicalRecordServiceInterface {

    private final MedicalRecordRepository repo;
    private final AppointmentRepository appointmentRepo;
    private final CustomerVisitRepository visitRepo;
    private final StaffInfoRepository staffRepo;
    private final VitalSignsRepository vitalRepo;
    private final QueueTicketRepository queueTicketRepo;
    private final Icd10CodeRepository icd10Repo;
    private final TestRequestRepository testRequestRepo;
    private final ProfileRepository profileRepo;
    private final InvoiceRepository invoiceRepo;

    @Transactional(readOnly = true)
    public PageResponse<MedicalRecordResponse> search(UUID doctorId, MedicalRecordStatus status,
                                                       LocalDateTime from, LocalDateTime to,
                                                       Pageable pageable) {
        Page<MedicalRecord> page = repo.search(doctorId, status, from, to, pageable);
        return PageResponse.from(page, r -> MedicalRecordResponse.from(r, false));
    }

    /**
     * API cho le tan tim kiem ho so benh an voi cac filter.
     */
    @Transactional(readOnly = true)
    public PageResponse<ReceptionistRecordResponse> searchForReceptionist(String search, String gender,
                                                                        String age, BloodType bloodType,
                                                                        Pageable pageable) {
        Page<MedicalRecord> page = repo.findAll(
                searchForReceptionistSpec(search, gender, age, bloodType),
                pageable
        );
        // Eager fetch visit, customer, appointment de tranh LazyInitializationException
        page.getContent().forEach(r -> {
            if (r.getVisit() != null) {
                r.getVisit().getCustomer();
                r.getVisit().getAppointment();
            }
        });
        return PageResponse.from(page, ReceptionistRecordResponse::from);
    }

    /**
     * API cho le tan lay danh sach customer (benh nhan) khong lap lai.
     * Moi customer chi xuat hien 1 lan trong danh sach (chi lay CUSTOMER, khong lay STAFF).
     */
    @Transactional(readOnly = true)
    public PageResponse<ReceptionistCustomerResponse> searchUniqueCustomers(String search, String gender,
                                                                         String age, BloodType bloodType,
                                                                         Pageable pageable) {
        Page<Profile> page = profileRepo.findAll(
                searchUniqueCustomerSpec(search, gender, age, bloodType),
                pageable
        );
        return PageResponse.from(page, ReceptionistCustomerResponse::from);
    }

    @Transactional(readOnly = true)
    public java.util.List<ReceptionistAllCustomerResponse> searchByPhone(String phone) {
        var result = new java.util.ArrayList<ReceptionistAllCustomerResponse>();

        // Tim trong Profile (chi lay CUSTOMER, khong lay STAFF)
        profileRepo.findFirstByPhone(phone).ifPresent(p -> {
            // Chi them neu account role la CUSTOMER
            if (p.getAccount() != null) {
                var role = p.getAccount().getRole();
                if (role == org.example.doansummer2026.enums.Role.CUSTOMER) {
                    result.add(ReceptionistAllCustomerResponse.forRegistered(
                            p.getProfileId(), p.getPhone(), p.getFullName(), p.getGender(),
                            p.getDateOfBirth(), p.getBloodType(), p.getEmail(), p.getAddress()
                    ));
                }
            } else {
                // Guest profile khong co account - them vao
                result.add(ReceptionistAllCustomerResponse.forGuest(
                        p.getPhone(), p.getFullName(), p.getGender(), p.getAddress()
                ));
            }
        });

        // Neu chua tim thay profile, tim trong Appointment (guest vang lai)
        if (result.isEmpty()) {
            var guestAppointments = appointmentRepo.findGuestAppointmentsByPhone(phone);
            var seenGuestInfo = new java.util.HashSet<String>();
            for (var a : guestAppointments) {
                var key = a.getGuestPhone() + "_" + a.getGuestFullName();
                if (!seenGuestInfo.contains(key)) {
                    result.add(ReceptionistAllCustomerResponse.forGuest(
                            a.getGuestPhone(),
                            a.getGuestFullName(),
                            a.getGuestGender(),
                            a.getGuestAddress()
                    ));
                    seenGuestInfo.add(key);
                }
            }
        }

        return result;
    }

    private org.springframework.data.jpa.domain.Specification<Profile> searchUniqueCustomerSpec(
            String search, String gender, String age, BloodType bloodType) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            // Chi lay CUSTOMER (account.role = CUSTOMER)
            var account = root.get("account");
            predicates.add(cb.equal(account.get("role"), org.example.doansummer2026.enums.Role.CUSTOMER));

            // Search theo ten, phone
            if (search != null && !search.isEmpty()) {
                String searchLower = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), searchLower),
                        cb.like(cb.lower(root.get("phone")), searchLower)
                ));
            }

            // Filter theo gioi tinh
            if (gender != null && !gender.isEmpty()) {
                predicates.add(cb.equal(root.get("gender"),
                        "Nam".equalsIgnoreCase(gender) ? Gender.MALE : Gender.FEMALE));
            }

            // Filter theo muc tuoi
            if (age != null && !age.isEmpty()) {
                LocalDate today = LocalDate.now();
                LocalDate fromDate = null;
                LocalDate toDate = null;

                switch (age) {
                    case "0-18":
                        toDate = today.minusYears(18);
                        break;
                    case "19-40":
                        fromDate = today.minusYears(40);
                        toDate = today.minusYears(19);
                        break;
                    case "41-60":
                        fromDate = today.minusYears(60);
                        toDate = today.minusYears(41);
                        break;
                    case "60+":
                        fromDate = today.minusYears(150);
                        toDate = today.minusYears(60);
                        break;
                }

                if (fromDate != null && toDate != null) {
                    predicates.add(cb.between(root.get("dateOfBirth"), fromDate, toDate));
                } else if (toDate != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("dateOfBirth"), toDate));
                } else if (fromDate != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("dateOfBirth"), fromDate));
                }
            }

            // Filter theo nhom mau
            if (bloodType != null) {
                predicates.add(cb.equal(root.get("bloodType"), bloodType));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private org.springframework.data.jpa.domain.Specification<MedicalRecord> searchForReceptionistSpec(
            String search, String gender, String age, BloodType bloodType) {
        return (root, query, cb) -> {
            var visit = root.get("visit");
            var customer = visit.get("customer");
            var appointment = visit.get("appointment");

            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            // Search theo ma ho so, ten benh nhan, so dien thoai (bao gom guest)
            if (search != null && !search.isEmpty()) {
                String searchLower = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("recordCode")), searchLower),
                        cb.like(cb.lower(customer.get("fullName")), searchLower),
                        cb.like(cb.lower(customer.get("phone")), searchLower),
                        cb.like(cb.lower(appointment.get("guestFullName")), searchLower),
                        cb.like(cb.lower(appointment.get("guestPhone")), searchLower)
                ));
            }

            // Filter theo gioi tinh
            if (gender != null && !gender.isEmpty()) {
                var genderPredicate = cb.or(
                        cb.and(
                                cb.equal(appointment.get("isGuest"), true),
                                cb.equal(appointment.get("guestGender"),
                                        "Nam".equalsIgnoreCase(gender) ? Gender.MALE : Gender.FEMALE)
                        ),
                        cb.equal(customer.get("gender"),
                                "Nam".equalsIgnoreCase(gender) ? Gender.MALE : Gender.FEMALE)
                );
                predicates.add(genderPredicate);
            }

            // Filter theo muc tuoi
            if (age != null && !age.isEmpty()) {
                LocalDate today = LocalDate.now();
                LocalDate fromDate = null;
                LocalDate toDate = null;

                switch (age) {
                    case "0-18":
                        toDate = today.minusYears(18); // < 18 tuoi
                        break;
                    case "19-40":
                        fromDate = today.minusYears(40);
                        toDate = today.minusYears(19);
                        break;
                    case "41-60":
                        fromDate = today.minusYears(60);
                        toDate = today.minusYears(41);
                        break;
                    case "60+":
                        fromDate = today.minusYears(150); // > 60 tuoi
                        toDate = today.minusYears(60);
                        break;
                }

                if (fromDate != null && toDate != null) {
                    predicates.add(cb.between(customer.get("dateOfBirth"), fromDate, toDate));
                } else if (toDate != null) {
                    predicates.add(cb.greaterThanOrEqualTo(customer.get("dateOfBirth"), toDate));
                } else if (fromDate != null) {
                    predicates.add(cb.lessThanOrEqualTo(customer.get("dateOfBirth"), fromDate));
                }
            }

            // Filter theo nhom mau - chi ap dung cho khach khong phai guest
            if (bloodType != null) {
                predicates.add(cb.equal(customer.get("bloodType"), bloodType));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    @Transactional(readOnly = true)
    public MedicalRecordResponse get(UUID id) {
        return MedicalRecordResponse.from(findById(id), true);
    }

    public synchronized MedicalRecordResponse create(MedicalRecordCreateRequest req) {
        CustomerVisit visit = visitRepo.findById(req.visitId())
                .orElseThrow(() -> new ResourceNotFoundException("Luot kham khong ton tai: " + req.visitId()));
        if (repo.findFirstByVisit_VisitIdAndQueueTicketIsNullOrderByCreatedAtDesc(req.visitId()).isPresent()) {
            throw new ConflictException("Luot kham da co ho so benh an doc lap");
        }
        StaffInfo doctor = staffRepo.findById(req.doctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Bac si khong ton tai: " + req.doctorId()));
        MedicalRecord r = MedicalRecord.builder()
                .visit(visit)
                .doctor(doctor)
                .chiefComplaint(req.chiefComplaint())
                .status(MedicalRecordStatus.IN_PROGRESS)
                .recordCode(generateRecordCode())
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

        return MedicalRecordResponse.from(repo.save(r), true);
    }

    private boolean hasVitalSigns(MedicalRecordCreateRequest req) {
        return req.bloodPressure() != null || req.heartRate() != null || req.temperature() != null ||
               req.weight() != null || req.height() != null;
    }

    public MedicalRecordResponse update(UUID id, MedicalRecordUpdateRequest req) {
        MedicalRecord r = findById(id);
        validateVersion(r, req);
        if (r.getStatus() == MedicalRecordStatus.COMPLETED) {
            throw new BadRequestException("Ho so da dong, khong the sua");
        }
        updateMedicalRecordFields(r, req);
        return MedicalRecordResponse.from(repo.save(r), true);
    }

    /**
     * Lục nháp - cập nhật dữ liệu và đồi status sang DRAFT.
     * Dùng khi bác sĩ đang nhập thông tin, chưa kết lục.
     */
    public MedicalRecordResponse saveDraft(UUID id, MedicalRecordUpdateRequest req) {
        MedicalRecord r = findById(id);
        validateVersion(r, req);
        if (r.getStatus() == MedicalRecordStatus.COMPLETED) {
            throw new BadRequestException("Ho so da dong, khong the luu nham");
        }
        boolean nurse = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_NURSE"));
        var actor = currentStaff().orElse(null);
        if (nurse && actor != null && r.getQueueTicket() != null
                && (actor.getDepartment() == null || !actor.getDepartment().getDepartmentId().equals(r.getQueueTicket().getDepartment().getDepartmentId())))
            throw new BadRequestException("Y ta chi duoc cap nhat ho so tai phong duoc phan cong");
        if (!nurse && actor != null && actor.getSystemRole().isDoctor()
                && r.getDoctor() != null && !r.getDoctor().getStaffId().equals(actor.getStaffId()))
            throw new BadRequestException("Ca kham nay thuoc bac si phu trach khac");
        if (nurse) {
            updateNursingDraftFields(r, req);
            if (actor != null) { r.setNursingUpdatedBy(actor); r.setNursingUpdatedAt(LocalDateTime.now()); }
        } else updateMedicalRecordFields(r, req);
        r.setStatus(MedicalRecordStatus.DRAFT);
        return MedicalRecordResponse.from(repo.save(r), true);
    }

    private void updateNursingDraftFields(MedicalRecord r, MedicalRecordUpdateRequest req) {
        if (req.chiefComplaint() != null) r.setChiefComplaint(req.chiefComplaint());
        if (req.clinicalFindings() != null) r.setClinicalFindings(req.clinicalFindings());
        if (r.getVitalSigns() == null && hasVitalSignsUpdate(req)) {
            r.setVitalSigns(VitalSigns.builder().medicalRecord(r).bloodPressure(req.bloodPressure())
                    .heartRate(req.heartRate()).temperature(req.temperature()).weight(req.weight()).height(req.height()).build());
        } else if (r.getVitalSigns() != null) {
            var v = r.getVitalSigns();
            if (req.bloodPressure()!=null) v.setBloodPressure(req.bloodPressure());
            if (req.heartRate()!=null) v.setHeartRate(req.heartRate());
            if (req.temperature()!=null) v.setTemperature(req.temperature());
            if (req.weight()!=null) v.setWeight(req.weight());
            if (req.height()!=null) v.setHeight(req.height());
        }
    }

    private void validateVersion(MedicalRecord record, MedicalRecordUpdateRequest req) {
        if (req != null && req.version() != null && !java.util.Objects.equals(req.version(), record.getVersion()))
            throw new ConflictException("Ho so da duoc nhan vien khac cap nhat. Vui long tai lai du lieu truoc khi luu");
    }

    private java.util.Optional<StaffInfo> currentStaff() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? java.util.Optional.empty() : staffRepo.findFirstByProfile_Account_Username(auth.getName());
    }

    private void updateMedicalRecordFields(MedicalRecord r, MedicalRecordUpdateRequest req) {
        if (req.chiefComplaint() != null) r.setChiefComplaint(req.chiefComplaint());
        if (req.clinicalFindings() != null) r.setClinicalFindings(req.clinicalFindings());
        if (req.diagnosis() != null) r.setDiagnosis(req.diagnosis());
        if (req.prescriptionNote() != null) r.setPrescriptionNote(req.prescriptionNote());
        if (req.conclusion() != null) r.setConclusion(req.conclusion());
        if (req.patientInstruction() != null) r.setPatientInstruction(req.patientInstruction());

        // Cap nhat thuoc trong don
        if (req.prescriptionItems() != null) {
            r.getPrescriptionItems().clear();
            req.prescriptionItems().forEach(p -> {
                // Chi them vao neu co du lieu hop le (tranh validation error)
                if (p.medicineName() != null && !p.medicineName().isBlank() && p.quantity() != null) {
                    PrescriptionItem item = PrescriptionItem.builder()
                            .medicalRecord(r)
                            .medicineName(p.medicineName())
                            .quantity(p.quantity())
                            .unit(p.unit())
                            .note(p.note())
                            .frequencyPerDay(p.frequencyPerDay())
                            .build();
                    r.getPrescriptionItems().add(item);
                }
            });
        }

        // Cap nhat benh chuan doan ICD-10
        if (req.icdSelections() != null) {
            r.getIcdSelections().clear();
            req.icdSelections().forEach(icd -> {
                // Uu tien su dung codeName tu request, fallback sang lookup DB
                String codeName = icd.codeName();
                if (codeName == null || codeName.isBlank()) {
                    Icd10Code icdCode = icd10Repo.findById(icd.code()).orElse(null);
                    codeName = icdCode != null ? icdCode.getName() : null;
                }
                Icd10Selection selection = Icd10Selection.builder()
                        .medicalRecord(r)
                        .code(icd.code())
                        .codeName(codeName)
                        .note(icd.note())
                        .build();
                r.getIcdSelections().add(selection);
            });
        }

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
    }

    private boolean hasVitalSignsUpdate(MedicalRecordUpdateRequest req) {
        return req.bloodPressure() != null || req.heartRate() != null || req.temperature() != null ||
               req.weight() != null || req.height() != null;
    }

    /** Kiem tra co TestRequest chua COMPLETED khong */
    private boolean checkTestRequestsCompletion(MedicalRecord record) {
        if (record.getVisit() == null) {
            return false;
        }
        long incompleteCount = testRequestRepo.countByMedicalRecordAndStatusIn(
                record.getRecordId(),
                java.util.List.of(TestRequestStatus.PENDING, TestRequestStatus.IN_PROGRESS));
        return incompleteCount > 0;
    }

    private boolean checkUnpaidInvoices(MedicalRecord record) {
        if (record.getRecordId() == null) return false;
        return invoiceRepo.findAllByMedicalRecord_RecordId(record.getRecordId()).stream()
                .anyMatch(inv -> inv.getStatus() == InvoiceStatus.PENDING);
    }

    public MedicalRecordResponse complete(UUID id, MedicalRecordUpdateRequest req) {
        MedicalRecord r = findById(id);
        validateVersion(r, req);
        var actor = currentStaff().orElse(null);
        if (actor != null && actor.getSystemRole().isDoctor() && r.getDoctor()!=null
                && !r.getDoctor().getStaffId().equals(actor.getStaffId()))
            throw new BadRequestException("Chi bac si phu trach moi duoc hoan thanh ca kham");
        if (r.getStatus() == MedicalRecordStatus.COMPLETED) {
            throw new BadRequestException("Ho so da duoc dong truoc do");
        }

        // Kiem tra tat ca TestRequest deu phai COMPLETED (neu co)
        // Neu co TestRequest chua COMPLETED -> tra loi loi
        boolean hasIncompleteTestRequests = checkTestRequestsCompletion(r);
        if (hasIncompleteTestRequests) {
            throw new BadRequestException("Con yeu cau xet nghiem chua hoan thanh");
        }

        // Kiem tra xem co hoa don nao chua thanh toan khong
        if (checkUnpaidInvoices(r)) {
            throw new BadRequestException("Benh nhan chua thanh toan hoa don xet nghiem/dich vu");
        }

        // Cap nhat thong tin medical record (icd-10, prescription, vitals, ...)
        if (req != null) {
            updateMedicalRecordFields(r, req);
        }

        // Kiem tra bat buoc nhap chan doan hoac ket luan
        boolean hasDiagnosis = r.getDiagnosis() != null && !r.getDiagnosis().trim().isEmpty();
        boolean hasConclusion = r.getConclusion() != null && !r.getConclusion().trim().isEmpty();
        boolean hasIcd10 = r.getIcdSelections() != null && !r.getIcdSelections().isEmpty();

        if (!hasDiagnosis && !hasConclusion && !hasIcd10) {
            throw new BadRequestException("Vui long nhap chan doan, ket luan hoac chon ma ICD-10 truoc khi hoan thanh ho so");
        }

        r.setStatus(MedicalRecordStatus.COMPLETED);
        r.setCompletedAt(LocalDateTime.now());
        if (actor != null) { r.setDoctorConfirmedBy(actor); r.setDoctorConfirmedAt(LocalDateTime.now()); }
        MedicalRecord saved = repo.save(r);

        // Tu dong cap nhat queue ticket sang DONE
        CustomerVisit visit = saved.getVisit();
        if (visit != null) {
            queueTicketRepo.findAllByVisit_VisitId(visit.getVisitId()).stream()
                    .filter(ticket -> ticket.getStatus() == QueueStatus.IN_PROGRESS)
                    .findFirst()
                    .ifPresent(ticket -> {
                        ticket.setStatus(QueueStatus.DONE);
                        ticket.setCompletedAt(LocalDateTime.now());
                        queueTicketRepo.save(ticket);
                    });
        }

        var fetched = repo.findById(saved.getRecordId()).orElse(saved);
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

    /**
     * Sinh ma so benh an tu dong: MR-YYYY-XXXXX (MR-nam-so thu tu).
     */
    private String generateRecordCode() {
        String year = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy"));
        String prefix = "MR-" + year + "-";

        // Tim ma so cuoi cung trong nam
        String lastCode = repo.findTopByRecordCodeStartingWithOrderByRecordCodeDesc(prefix);
        int nextNum = 1;
        if (lastCode != null) {
            try {
                String numStr = lastCode.substring(prefix.length());
                nextNum = Math.addExact(Integer.parseInt(numStr), 1);
            } catch (RuntimeException ex) {
                throw new IllegalStateException("Ma ho so gan nhat khong dung dinh dang: " + lastCode, ex);
            }
        }

        return prefix + String.format("%05d", nextNum);
    }

    /**
     * Lich su kham benh cua benh nhan.
     */
    @Transactional(readOnly = true)
    public PageResponse<MedicalHistoryResponse> getMedicalHistoryForPatient(UUID profileId, String search, Pageable pageable) {
        var spec = searchMedicalHistorySpec(profileId, search);
        var page = repo.findAll(spec, pageable);

        return PageResponse.from(page, MedicalHistoryResponse::from);
    }

    private org.springframework.data.jpa.domain.Specification<MedicalRecord> searchMedicalHistorySpec(
            UUID profileId, String search) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            var visit = root.get("visit");
            var customer = visit.get("customer");

            if (profileId != null) {
                predicates.add(cb.equal(customer.get("profileId"), profileId));
            }

            // Chi lay nhung record da hoan thanh
            predicates.add(cb.equal(root.get("status"), MedicalRecordStatus.COMPLETED));

            if (search != null && !search.isBlank()) {
                String searchLower = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("recordCode")), searchLower),
                        cb.like(cb.lower(root.get("diagnosis")), searchLower),
                        cb.like(cb.lower(visit.get("checkInTime")), searchLower)
                ));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    /**
     * Chi tiet luot kham cua benh nhan (theo visitId).
     */
    @Transactional(readOnly = true)
    public org.example.doansummer2026.dto.medicalHistory.VisitDetailResponse getVisitDetail(UUID visitId, UUID profileId) {
        org.example.doansummer2026.model.MedicalRecord record = repo.findFirstByVisit_VisitIdOrderByCreatedAtDesc(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Ho so khong ton tai: " + visitId));

        // Kiem tra quyen so huu
        if (record.getVisit() == null || record.getVisit().getCustomer() == null
                || !record.getVisit().getCustomer().getProfileId().equals(profileId)) {
            throw new ResourceNotFoundException("Khong tim thay ho so");
        }

        return org.example.doansummer2026.dto.medicalHistory.VisitDetailResponse.from(
                repo.findAllByVisit_VisitIdOrderByCreatedAtAsc(record.getVisit().getVisitId()));
    }

    /**
     * Chi tiet luot kham cua benh nhan (theo recordId).
     */
    @Transactional(readOnly = true)
    public org.example.doansummer2026.dto.medicalHistory.VisitDetailResponse getVisitDetailByRecordId(UUID recordId, UUID profileId) {
        org.example.doansummer2026.model.MedicalRecord record = repo.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Ho so khong ton tai: " + recordId));

        // Kiem tra quyen so huu
        if (record.getVisit() == null || record.getVisit().getCustomer() == null
                || !record.getVisit().getCustomer().getProfileId().equals(profileId)) {
            throw new ResourceNotFoundException("Khong tim thay ho so");
        }

        return org.example.doansummer2026.dto.medicalHistory.VisitDetailResponse.from(
                repo.findAllByVisit_VisitIdOrderByCreatedAtAsc(record.getVisit().getVisitId()));
    }

    /**
     * Danh gia phong kham (1-5 sao). Chi ap dung cho EXAMINATION da hoan thanh (COMPLETED).
     */
    public MedicalRecordResponse rate(UUID id, int ratingScore) {
        if (ratingScore < 1 || ratingScore > 5) {
            throw new BadRequestException("Diem danh gia phai tu 1-5 sao");
        }
        MedicalRecord r = findById(id);
        if (r.getStatus() != MedicalRecordStatus.COMPLETED) {
            throw new BadRequestException("Chi co the danh gia phieu da hoan thanh (COMPLETED), hien tai: " + r.getStatus());
        }
        r.setRatingScore(ratingScore);
        r.setRatedAt(LocalDateTime.now());
        return MedicalRecordResponse.from(repo.save(r), true);
    }

    public org.example.doansummer2026.dto.medicalRecord.FeedbackResponse submitFeedback(
            UUID id, UUID profileId, org.example.doansummer2026.dto.medicalRecord.FeedbackRequest req) {
        MedicalRecord r = findById(id);
        if (r.getVisit() == null || r.getVisit().getCustomer() == null
                || !r.getVisit().getCustomer().getProfileId().equals(profileId)) {
            throw new ResourceNotFoundException("Khong tim thay ho so");
        }
        if (r.getStatus() != MedicalRecordStatus.COMPLETED) throw new BadRequestException("Chi danh gia dich vu da hoan thanh");
        r.setRatingScore(req.overallRating());
        r.setDoctorRating(null); r.setWaitingRating(null); r.setStaffRating(null);
        r.setRatingComment(req.comment()); r.setContactRequested(false);
        r.setRatedAt(LocalDateTime.now()); r.setFeedbackStatus("NEW");
        r.getFeedbackTargets().clear();
        return org.example.doansummer2026.dto.medicalRecord.FeedbackResponse.from(repo.save(r));
    }

    @Transactional(readOnly = true)
    public PageResponse<org.example.doansummer2026.dto.medicalRecord.FeedbackResponse> listFeedbacks(UUID doctorId, Pageable pageable) {
        var page = doctorId == null ? repo.findByRatingScoreIsNotNull(pageable)
                : repo.findFeedbacksForStaff(doctorId, pageable);
        return PageResponse.from(page, org.example.doansummer2026.dto.medicalRecord.FeedbackResponse::from);
    }

    public org.example.doansummer2026.dto.medicalRecord.FeedbackResponse respondFeedback(
            UUID id, UUID staffId, String response, String internalNote, String status) {
        MedicalRecord r = findById(id);
        if (response != null) { r.setManagerResponse(response); r.setRespondedAt(LocalDateTime.now());
            if (staffId != null) r.setRespondedBy(staffRepo.findById(staffId).orElse(null)); }
        if (internalNote != null) r.setInternalNote(internalNote);
        r.setFeedbackStatus(status != null ? status : (response != null ? "RESPONDED" : "IN_REVIEW"));
        return org.example.doansummer2026.dto.medicalRecord.FeedbackResponse.from(repo.save(r));
    }

    public org.example.doansummer2026.dto.medicalRecord.FeedbackResponse explainFeedback(UUID id, UUID doctorId, String explanation) {
        MedicalRecord r = findById(id);
        boolean related = r.getDoctor() != null && r.getDoctor().getStaffId().equals(doctorId)
                || r.getFeedbackTargets().stream().anyMatch(t -> t.getStaff() != null && t.getStaff().getStaffId().equals(doctorId));
        if (!related) throw new ResourceNotFoundException("Danh gia khong thuoc bac si nay");
        r.setDoctorExplanation(explanation); r.setFeedbackStatus("WAITING_INTERNAL");
        return org.example.doansummer2026.dto.medicalRecord.FeedbackResponse.from(repo.save(r));
    }

    /** Lấy danh sách yêu cầu tái khám (follow-up) chưa đặt lịch cho lễ tân */
    public PageResponse<org.example.doansummer2026.dto.medicalRecord.FollowUpResponse> getPendingFollowUps(Pageable pageable) {
        Page<MedicalRecord> page = repo.findPendingFollowUps(pageable);
        return PageResponse.from(page, org.example.doansummer2026.dto.medicalRecord.FollowUpResponse::from);
    }

    /** Tạo lịch hẹn từ yêu cầu tái khám và cập nhật vào hồ sơ bệnh án */
    public org.example.doansummer2026.dto.medicalRecord.FollowUpResponse scheduleFollowUp(UUID recordId, org.example.doansummer2026.dto.appointment.AppointmentCreateRequest req) {
        MedicalRecord record = findById(recordId);
        if (record.getFollowUpDate() == null && (record.getFollowUpNote() == null || record.getFollowUpNote().trim().isEmpty())) {
            throw new BadRequestException("Hồ sơ này không có yêu cầu tái khám");
        }
        if (record.getFollowUpAppointment() != null) {
            throw new ConflictException("Yêu cầu tái khám này đã được đặt lịch hẹn");
        }

        // Tạo Appointment trực tiếp thay vì qua AppointmentService.create để hỗ trợ cả Guest và Profile không có Account
        var originalVisit = record.getVisit();
        var customer = originalVisit.getCustomer();
        var oldAppt = originalVisit.getAppointment();

        org.example.doansummer2026.model.Appointment appointment = org.example.doansummer2026.model.Appointment.builder()
                .scheduledAt(req.scheduledAt())
                .timeSlot(req.timeSlot())
                .status(org.example.doansummer2026.enums.AppointmentStatus.PENDING)
                .build();

        if (customer != null) {
            appointment.setCustomer(customer);
        } else if (oldAppt != null && Boolean.TRUE.equals(oldAppt.getIsGuest())) {
            appointment.setIsGuest(true);
            appointment.setGuestFullName(oldAppt.getGuestFullName());
            appointment.setGuestPhone(oldAppt.getGuestPhone());
            appointment.setGuestAge(oldAppt.getGuestAge());
            appointment.setGuestGender(oldAppt.getGuestGender());
            appointment.setGuestAddress(oldAppt.getGuestAddress());
        } else {
            appointment.setIsGuest(true);
            appointment.setGuestFullName("Khách vãng lai");
        }

        appointment = appointmentRepo.save(appointment);
        
        // Cập nhật MedicalRecord với ID của lịch hẹn vừa tạo
        record.setFollowUpAppointment(appointment);
        
        return org.example.doansummer2026.dto.medicalRecord.FollowUpResponse.from(repo.save(record));
    }
}
