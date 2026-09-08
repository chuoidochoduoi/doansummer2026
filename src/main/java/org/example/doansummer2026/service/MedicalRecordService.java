package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.icd.ICD10SelectionCreateRequest;
import org.example.doansummer2026.dto.medicalrecord.MedicalRecordCreateRequest;
import org.example.doansummer2026.dto.medicalrecord.MedicalRecordResponse;
import org.example.doansummer2026.dto.medicalrecord.MedicalRecordUpdateRequest;
import org.example.doansummer2026.dto.medicalrecord.ReceptionistRecordResponse;
import org.example.doansummer2026.dto.medicalrecord.ReceptionistCustomerResponse;
import org.example.doansummer2026.dto.medicalrecord.ReceptionistAllCustomerResponse;
import org.example.doansummer2026.dto.medicalhistory.MedicalHistoryResponse;
import org.example.doansummer2026.dto.medicalhistory.VisitHistorySummaryResponse;
import org.example.doansummer2026.enums.BloodType;
import org.example.doansummer2026.enums.Gender;
import org.example.doansummer2026.dto.medicalrecord.PrescriptionItemCreateRequest;
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
import org.example.doansummer2026.repository.MedicalServiceRepository;
import org.example.doansummer2026.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
@Slf4j
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
    private final MedicalServiceRepository medicalServiceRepo;
    private final org.example.doansummer2026.repository.ShiftConfigRepository shiftConfigRepository;
    private final ShiftScheduleResolver shiftScheduleResolver;
    private final NotificationService notificationService;
    private final AuthService authService;
    private final ClinicalFormTemplateService clinicalFormTemplateService;
    private final org.example.doansummer2026.repository.TestResultRevisionRepository testResultRevisionRepo;
    private final org.example.doansummer2026.repository.TestResultAttachmentRepository testResultAttachmentRepo;
    private final SameDayParaclinicalResultService sameDayParaclinicalResultService;
    private final StaffDutyService staffDutyService;

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
    public ReceptionistCustomerResponse getCustomerForReceptionist(UUID customerId) {
        Profile profile = profileRepo.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bệnh nhân"));
        return ReceptionistCustomerResponse.from(profile);
    }

    @Transactional(readOnly = true)
    public java.util.List<ReceptionistAllCustomerResponse> searchByPhone(String phone) {
        var result = new java.util.ArrayList<ReceptionistAllCustomerResponse>();
        String normalizedPhone = normalizeReceptionPhone(phone);
        java.util.List<String> phoneValues = normalizedPhone == null
                ? java.util.List.of()
                : java.util.List.of(normalizedPhone, "+84" + normalizedPhone.substring(1));

        // Tim trong Profile (chi lay CUSTOMER, khong lay STAFF)
        profileRepo.findFirstByPhoneIn(phoneValues).ifPresent(p -> {
            // Chi them neu account role la CUSTOMER
            if (p.getAccount() != null) {
                var role = p.getAccount().getRole();
                if (role == org.example.doansummer2026.enums.Role.CUSTOMER) {
                    result.add(ReceptionistAllCustomerResponse.forRegistered(
                            p.getProfileId(), p.getPatientCode(), p.getPhone(), p.getFullName(), p.getGender(),
                            p.getDateOfBirth(), p.getBloodType(), p.getEmail(), p.getAddress()
                    ));
                }
            } else {
                // Guest profile khong co account - them vao
                result.add(ReceptionistAllCustomerResponse.forGuest(
                        p.getProfileId(), p.getPatientCode(), p.getPhone(), p.getFullName(), p.getGender(),
                        p.getDateOfBirth(), p.getBloodType(), p.getEmail(), p.getAddress()
                ));
            }
        });

        // Neu chua tim thay profile, tim trong Appointment (guest vang lai)
        if (result.isEmpty()) {
            var guestAppointments = phoneValues.stream()
                    .flatMap(value -> appointmentRepo.findGuestAppointmentsByPhone(value).stream())
                    .toList();
            var seenGuestInfo = new java.util.HashSet<String>();
            for (var a : guestAppointments) {
                var key = a.getGuestPhone() + "_" + a.getGuestFullName();
                if (!seenGuestInfo.contains(key)) {
                    result.add(ReceptionistAllCustomerResponse.forGuest(
                            null,
                            null,
                            a.getGuestPhone(),
                            a.getGuestFullName(),
                            a.getGuestGender(),
                            null,
                            null,
                            a.getGuestEmail(),
                            a.getGuestAddress()
                    ));
                    seenGuestInfo.add(key);
                }
            }
        }

        return result;
    }

    private String normalizeReceptionPhone(String value) {
        if (value == null) return null;
        String phone = value.trim().replaceAll("[\\s.-]", "");
        if (phone.isBlank()) return null;
        return phone.startsWith("+84") ? "0" + phone.substring(3) : phone;
    }

    private org.springframework.data.jpa.domain.Specification<Profile> searchUniqueCustomerSpec(
            String search, String gender, String age, BloodType bloodType) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            // Bao gồm cả bệnh nhân có tài khoản và hồ sơ khách vãng lai đã từng khám.
            var account = root.join("account", jakarta.persistence.criteria.JoinType.LEFT);
            predicates.add(cb.or(
                    cb.isNull(account.get("accountId")),
                    cb.equal(account.get("role"), org.example.doansummer2026.enums.Role.CUSTOMER)
            ));

            // Search theo ten, phone, email hoac ma benh nhan.
            if (search != null && !search.isEmpty()) {
                String searchLower = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), searchLower),
                        cb.like(cb.lower(root.get("phone")), searchLower),
                        cb.like(cb.lower(root.get("email")), searchLower),
                        cb.like(cb.lower(root.get("patientCode")), searchLower)
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

    public MedicalRecordResponse create(MedicalRecordCreateRequest req) {
        CustomerVisit visit = visitRepo.findById(req.visitId())
                .orElseThrow(() -> new ResourceNotFoundException("Lượt khám không tồn tại: " + req.visitId()));
        if (repo.findFirstByVisit_VisitIdAndQueueTicketIsNullOrderByCreatedAtDesc(req.visitId()).isPresent()) {
            throw new ConflictException("Lượt khám đã có hồ sơ bệnh án độc lập");
        }
        StaffInfo doctor = staffRepo.findById(req.doctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Bác sĩ không tồn tại: " + req.doctorId()));
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
                    .orElseThrow(() -> new ResourceNotFoundException("Nhân viên không tồn tại: " + req.recordedById()));
            VitalSigns v = VitalSigns.builder()
                    .medicalRecord(r)
                    .bloodPressure(req.bloodPressure())
                    .heartRate(req.heartRate())
                    .temperature(req.temperature())
                    .weight(req.weight())
                    .height(req.height())
                    .recordedAt(LocalDateTime.now())
                    .recordedBy(recordedBy)
                    .build();
            r.setVitalSigns(v);
        }

        return MedicalRecordResponse.from(repo.saveAndFlush(r), true);
    }

    private boolean hasVitalSigns(MedicalRecordCreateRequest req) {
        return req.bloodPressure() != null || req.heartRate() != null || req.temperature() != null ||
               req.weight() != null || req.height() != null;
    }

    public MedicalRecordResponse update(UUID id, MedicalRecordUpdateRequest req) {
        MedicalRecord r = findById(id);
        if (r.getStatus() == MedicalRecordStatus.COMPLETED) {
            throw new ConflictException("Không thể cập nhật hồ sơ đã hoàn thành");
        }
        validateVersion(r, req);
        ensureDoctorCanEdit(r);
        updateMedicalRecordFields(r, req);
        return MedicalRecordResponse.from(repo.saveAndFlush(r), true);
    }

    /**
     * Luu nhap - cap nhat du lieu va doi status sang DRAFT.
     * Dung khi bac si dang nhap thong tin, chua ket luan.
     */
    public MedicalRecordResponse saveDraft(UUID id, MedicalRecordUpdateRequest req) {
        MedicalRecord r = findById(id);
        if (r.getStatus() == MedicalRecordStatus.COMPLETED) {
            throw new ConflictException("Không thể cập nhật hồ sơ đã hoàn thành");
        }
        validateVersion(r, req);
        var actor = currentStaff().orElse(null);
        if (actor == null) {
            throw new BadRequestException("Không xác định được nhân viên đang thao tác");
        }
        boolean nurse = actor.getSystemRole() == org.example.doansummer2026.enums.SystemRole.NURSE;
        if (nurse && r.getQueueTicket() == null) {
            throw new BadRequestException("Y tá chỉ được lưu hồ sơ tại phòng đang được phân công");
        }
        if (nurse && r.getQueueTicket() != null) {
            staffDutyService.requireCurrentStaffOnDuty(r.getQueueTicket().getDepartment(), false);
        }
        if (!nurse) ensureDoctorCanEdit(r);
        if (nurse) {
            updateNursingDraftFields(r, req);
            if (actor != null) { r.setNursingUpdatedBy(actor); r.setNursingUpdatedAt(LocalDateTime.now()); }
        } else updateMedicalRecordFields(r, req);
        r.setStatus(MedicalRecordStatus.DRAFT);
        return MedicalRecordResponse.from(repo.saveAndFlush(r), true);
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
            throw new ConflictException("Hồ sơ đã được nhân viên khác cập nhật. Vui lòng tải lại dữ liệu trước khi lưu");
    }

    private java.util.Optional<StaffInfo> currentStaff() {
        UUID staffId = authService.currentStaffId();
        return staffId == null ? java.util.Optional.empty() : staffRepo.findById(staffId);
    }

    private void ensureDoctorCanEdit(MedicalRecord record) {
        StaffInfo actor = currentStaff()
                .orElseThrow(() -> new BadRequestException("Không xác định được bác sĩ đang thao tác"));
        if (!actor.getSystemRole().isDoctor()) {
            throw new BadRequestException("Chỉ bác sĩ điều trị mới được cập nhật hồ sơ khám");
        }
        UUID responsibleDoctorId = record.getDoctor() != null ? record.getDoctor().getStaffId() : null;
        if (responsibleDoctorId == null || !responsibleDoctorId.equals(actor.getStaffId())) {
            throw new BadRequestException("Ca khám này thuộc bác sĩ điều trị khác");
        }
    }

    private void updateMedicalRecordFields(MedicalRecord r, MedicalRecordUpdateRequest req) {
        validatePrescriptionAllergyStatus(r, req.prescriptionItems());
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
                    .recordedAt(LocalDateTime.now())
                    .recordedBy(r.getDoctor())
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

    @Transactional(readOnly = true)
    public org.example.doansummer2026.dto.clinicalform.ResolvedClinicalFormResponse getClinicalForm(UUID recordId) {
        MedicalRecord record = findById(recordId);
        if (record.getQueueTicket() == null || record.getQueueTicket().getService() == null)
            throw new ResourceNotFoundException("Hồ sơ không có biểu mẫu động");
        var version = record.getFormTemplateVersion() != null ? record.getFormTemplateVersion()
                : clinicalFormTemplateService.resolveVersion(record.getQueueTicket().getService().getServiceId(), null);
        return clinicalFormTemplateService.resolvedResponse(version, record.getSpecialtyData());
    }

    @Transactional(readOnly = true)
    public org.example.doansummer2026.dto.clinicalform.ResolvedClinicalFormResponse getClinicalFormForPatient(
            UUID recordId, UUID profileId) {
        MedicalRecord record = findById(recordId);
        UUID ownerId = record.getVisit() == null || record.getVisit().getCustomer() == null
                ? null : record.getVisit().getCustomer().getProfileId();
        if (profileId == null || !profileId.equals(ownerId))
            throw new org.springframework.security.access.AccessDeniedException("Không có quyền xem biểu mẫu hồ sơ này");
        return getClinicalForm(recordId);
    }

    @Transactional(readOnly = true)
    public org.example.doansummer2026.dto.medicalrecord.PatientAllergyResponse getPatientAllergies(UUID recordId) {
        return org.example.doansummer2026.dto.medicalrecord.PatientAllergyResponse.from(patientProfile(findById(recordId)));
    }

    public org.example.doansummer2026.dto.medicalrecord.PatientAllergyResponse updatePatientAllergies(
            UUID recordId, org.example.doansummer2026.dto.medicalrecord.PatientAllergyRequest request) {
        MedicalRecord record = findById(recordId);
        Profile profile = patientProfile(record);
        if (profile == null) {
            throw new BadRequestException("Bệnh nhân vãng lai chưa có hồ sơ để cập nhật dị ứng");
        }
        if (request.status() == org.example.doansummer2026.enums.AllergyStatus.UNVERIFIED) {
            throw new BadRequestException("Vui lòng xác nhận không ghi nhận dị ứng hoặc nhập ít nhất một dị ứng");
        }
        var items = org.example.doansummer2026.dto.medicalrecord.PatientAllergyResponse.normalize(request.items());
        if (request.status() == org.example.doansummer2026.enums.AllergyStatus.REPORTED && items.isEmpty()) {
            throw new BadRequestException("Vui lòng nhập ít nhất một dị ứng");
        }
        if (request.status() == org.example.doansummer2026.enums.AllergyStatus.NONE_REPORTED && !items.isEmpty()) {
            throw new BadRequestException("Không thể vừa xác nhận không dị ứng vừa gửi danh sách dị ứng");
        }
        profile.setAllergies(request.status() == org.example.doansummer2026.enums.AllergyStatus.NONE_REPORTED
                ? "" : String.join("\n", items));
        return org.example.doansummer2026.dto.medicalrecord.PatientAllergyResponse.from(profileRepo.save(profile));
    }

    public void validatePrescriptionAllergyStatus(MedicalRecord record,
            java.util.Collection<PrescriptionItemCreateRequest> requestedItems) {
        if (requestedItems == null || requestedItems.stream().noneMatch(item -> item != null
                && item.medicineName() != null && !item.medicineName().isBlank()
                && item.quantity() != null)) return;
        Profile profile = patientProfile(record);
        if (profile != null && profile.getAllergies() == null) {
            throw new BadRequestException("Vui lòng xác minh dị ứng của bệnh nhân trước khi lưu đơn thuốc");
        }
    }

    public void ensureAllergiesVerifiedForExistingPrescription(MedicalRecord record) {
        if (record.getPrescriptionItems() == null || record.getPrescriptionItems().isEmpty()) return;
        Profile profile = patientProfile(record);
        if (profile != null && profile.getAllergies() == null) {
            throw new BadRequestException("Vui lòng xác minh dị ứng của bệnh nhân trước khi hoàn tất hồ sơ có đơn thuốc");
        }
    }

    public void validateVitalSignsForCompletion(MedicalRecord record) {
        VitalSigns vitalSigns = record.getVitalSigns();
        java.util.List<String> errors = new java.util.ArrayList<>();

        if (vitalSigns == null) {
            throw new BadRequestException(
                    "Vui lòng nhập đầy đủ nhịp tim, huyết áp, thân nhiệt, chiều cao và cân nặng trước khi hoàn thành");
        }

        Integer heartRate = vitalSigns.getHeartRate();
        if (heartRate == null) errors.add("nhịp tim đang thiếu");
        else if (heartRate < 30 || heartRate > 220) errors.add("nhịp tim phải từ 30 đến 220 BPM");

        String bloodPressure = vitalSigns.getBloodPressure();
        if (bloodPressure == null || bloodPressure.isBlank()) errors.add("huyết áp đang thiếu");
        else {
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("^(\\d{2,3})\\s*/\\s*(\\d{2,3})$")
                    .matcher(bloodPressure.trim());
            if (!matcher.matches()) errors.add("huyết áp phải theo định dạng SYS/DIA");
            else {
                int systolic = Integer.parseInt(matcher.group(1));
                int diastolic = Integer.parseInt(matcher.group(2));
                if (systolic < 60 || systolic > 250) errors.add("huyết áp tâm thu phải từ 60 đến 250 mmHg");
                if (diastolic < 40 || diastolic > 150) errors.add("huyết áp tâm trương phải từ 40 đến 150 mmHg");
                if (systolic - diastolic < 10) errors.add("huyết áp tâm thu phải lớn hơn tâm trương ít nhất 10 mmHg");
            }
        }

        BigDecimal temperature = vitalSigns.getTemperature();
        if (temperature == null) errors.add("thân nhiệt đang thiếu");
        else if (temperature.compareTo(new BigDecimal("34.0")) < 0
                || temperature.compareTo(new BigDecimal("43.0")) > 0
                || !hasAtMostOneDecimal(temperature))
            errors.add("thân nhiệt phải từ 34,0 đến 43,0 °C và có tối đa 1 số thập phân");

        BigDecimal height = vitalSigns.getHeight();
        if (height == null) errors.add("chiều cao đang thiếu");
        else if (height.compareTo(new BigDecimal("30")) < 0 || height.compareTo(new BigDecimal("250")) > 0)
            errors.add("chiều cao phải từ 30 đến 250 cm");

        BigDecimal weight = vitalSigns.getWeight();
        if (weight == null) errors.add("cân nặng đang thiếu");
        else if (weight.compareTo(new BigDecimal("1.0")) < 0
                || weight.compareTo(new BigDecimal("300.0")) > 0
                || !hasAtMostOneDecimal(weight))
            errors.add("cân nặng phải từ 1,0 đến 300,0 kg và có tối đa 1 số thập phân");

        if (!errors.isEmpty()) {
            throw new BadRequestException("Chỉ số sinh hiệu không hợp lệ: " + String.join("; ", errors));
        }
    }

    /**
     * Tao mot ban sao dau hieu sinh ton cho lan kham tiep theo trong cung VIS.
     * Moi benh an van so huu mot ban ghi rieng; du lieu nguon khong bi thay doi
     * neu bac si do lai va cap nhat benh an hien tai.
     */
    public MedicalRecord inheritFirstVisitVitalSigns(MedicalRecord target) {
        if (target == null || target.getVitalSigns() != null || target.getVisit() == null
                || target.getVisit().getVisitId() == null) {
            return target;
        }

        MedicalRecord source = repo.findAllByVisit_VisitIdOrderByCreatedAtAsc(
                        target.getVisit().getVisitId()).stream()
                .filter(record -> record != null
                        && (target.getRecordId() == null
                        || !target.getRecordId().equals(record.getRecordId())))
                .filter(record -> record.getStatus() == MedicalRecordStatus.COMPLETED)
                .filter(record -> record.getQueueTicket() != null
                        && record.getQueueTicket().getDepartment() != null
                        && record.getQueueTicket().getDepartment().getDepartmentType() != null
                        && record.getQueueTicket().getDepartment().getDepartmentType().normalized()
                        == org.example.doansummer2026.enums.DepartmentType.EXAMINATION)
                .filter(this::hasValidVitalSigns)
                .findFirst()
                .orElse(null);
        if (source == null) return target;

        VitalSigns sourceVitals = source.getVitalSigns();
        VitalSigns inherited = VitalSigns.builder()
                .medicalRecord(target)
                .bloodPressure(sourceVitals.getBloodPressure())
                .heartRate(sourceVitals.getHeartRate())
                .temperature(sourceVitals.getTemperature())
                .weight(sourceVitals.getWeight())
                .height(sourceVitals.getHeight())
                .recordedAt(sourceVitals.getRecordedAt())
                .recordedBy(sourceVitals.getRecordedBy())
                .build();
        target.setVitalSigns(inherited);
        return repo.save(target);
    }

    private boolean hasValidVitalSigns(MedicalRecord record) {
        try {
            validateVitalSignsForCompletion(record);
            return true;
        } catch (BadRequestException ignored) {
            return false;
        }
    }

    private boolean hasAtMostOneDecimal(BigDecimal value) {
        return value.stripTrailingZeros().scale() <= 1;
    }

    private Profile patientProfile(MedicalRecord record) {
        return record.getVisit() == null ? null : record.getVisit().getCustomer();
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
                java.util.List.of(TestRequestStatus.PENDING, TestRequestStatus.IN_PROGRESS, TestRequestStatus.BLOCKED));
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
        if (actor == null) {
            throw new BadRequestException("Không xác định được bác sĩ đang thao tác");
        }
        UUID responsibleDoctorId = r.getDoctor() != null ? r.getDoctor().getStaffId() : null;
        if (!actor.getSystemRole().isDoctor()
                || responsibleDoctorId == null
                || !responsibleDoctorId.equals(actor.getStaffId())) {
            throw new BadRequestException("Chỉ bác sĩ đã bắt đầu bệnh án mới được hoàn thành ca khám");
        }
        if (r.getStatus() == MedicalRecordStatus.COMPLETED) {
            throw new BadRequestException("Hồ sơ đã được đóng trước đó");
        }

        // Kiem tra tat ca TestRequest deu phai COMPLETED (neu co)
        // Neu co TestRequest chua COMPLETED -> tra loi loi
        boolean hasIncompleteTestRequests = checkTestRequestsCompletion(r);
        if (hasIncompleteTestRequests) {
            throw new BadRequestException("Còn yêu cầu cận lâm sàng chưa hoàn thành");
        }

        // Kiem tra xem co hoa don nao chua thanh toan khong
        if (checkUnpaidInvoices(r)) {
            throw new BadRequestException("Bệnh nhân chưa thanh toán hóa đơn cận lâm sàng/dịch vụ");
        }

        // Cap nhat thong tin medical record (icd-10, prescription, vitals, ...)
        if (req != null) {
            updateMedicalRecordFields(r, req);
        }
        validateVitalSignsForCompletion(r);
        ensureAllergiesVerifiedForExistingPrescription(r);

        // Kiem tra bat buoc nhap chan doan hoac ket luan
        boolean hasDiagnosis = r.getDiagnosis() != null && !r.getDiagnosis().trim().isEmpty();
        boolean hasConclusion = r.getConclusion() != null && !r.getConclusion().trim().isEmpty();
        boolean hasIcd10 = r.getIcdSelections() != null && !r.getIcdSelections().isEmpty();

        if (!hasDiagnosis && !hasConclusion && !hasIcd10) {
            throw new BadRequestException("Vui lòng nhập chẩn đoán, kết luận hoặc chọn mã ICD-10 trước khi hoàn thành hồ sơ");
        }

        r.setStatus(MedicalRecordStatus.COMPLETED);
        r.setCompletedAt(LocalDateTime.now());
        if (actor != null) { r.setDoctorConfirmedBy(actor); r.setDoctorConfirmedAt(LocalDateTime.now()); }
        MedicalRecord saved = repo.save(r);
        
        String patientName = r.getVisit() != null && r.getVisit().getAppointment() != null ? 
            (r.getVisit().getAppointment().getIsGuest() != null && r.getVisit().getAppointment().getIsGuest() ? 
                r.getVisit().getAppointment().getGuestFullName() : 
                (r.getVisit().getAppointment().getCustomer() != null ? r.getVisit().getAppointment().getCustomer().getFullName() : "Khách")) : "Khách";
        
        notificationService.notifyStaffByRole(
            org.example.doansummer2026.enums.SystemRole.RECEPTIONIST,
            "Khám bệnh hoàn tất", 
            String.format("Bác sĩ đã khám xong cho bệnh nhân %s.", patientName), 
            "MedicalRecord", 
            saved.getRecordId()
        );

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
            throw new ResourceNotFoundException("Hồ sơ không tồn tại: " + id);
        }
        throw new ConflictException("Không thể xóa hồ sơ bệnh án. Hồ sơ nháp được tiếp tục chỉnh sửa; hồ sơ hoàn thành phải được lưu lịch sử");
    }

    public MedicalRecord findById(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hồ sơ không tồn tại: " + id));
    }

    /**
     * Sinh ma ho so khong phu thuoc MAX+1, an toan khi chay nhieu backend instance.
     */
    private String generateRecordCode() {
        String year = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy"));
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        return "MR-" + year + "-" + suffix;
    }

    /**
     * Lich su kham benh cua benh nhan.
     */
    @Transactional(readOnly = true)
    public PageResponse<MedicalHistoryResponse> getMedicalHistoryForPatient(UUID profileId, String search, Pageable pageable) {
        var spec = searchMedicalHistorySpec(profileId, search);
        Pageable sorted = pageable.getSort().isSorted() ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        var page = repo.findAll(spec, sorted);

        return PageResponse.from(page, MedicalHistoryResponse::from);
    }

    /** Danh sach lich su theo luot kham; mot CustomerVisit chi xuat hien mot lan. */
    @Transactional(readOnly = true)
    public PageResponse<VisitHistorySummaryResponse> getVisitHistoryForPatient(
            UUID profileId, String search, Pageable pageable) {
        Pageable sorted = pageable.getSort().isSorted() ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "checkInTime"));
        Page<CustomerVisit> page = visitRepo.findMedicalHistoryVisits(
                profileId, search == null ? "" : search.trim(), sorted);
        return PageResponse.from(page, this::toVisitHistorySummary);
    }

    private VisitHistorySummaryResponse toVisitHistorySummary(CustomerVisit visit) {
        java.util.List<MedicalRecord> examinations = repo
                .findAllByVisit_VisitIdOrderByCreatedAtAsc(visit.getVisitId()).stream()
                .filter(record -> record.getStatus() == MedicalRecordStatus.COMPLETED)
                .filter(record -> record.getQueueTicket() != null
                        && record.getQueueTicket().getDepartment() != null
                        && record.getQueueTicket().getDepartment().getDepartmentType()
                        == org.example.doansummer2026.enums.DepartmentType.EXAMINATION)
                .toList();
        java.util.List<org.example.doansummer2026.model.TestRequest> requests =
                testRequestRepo.findAllByVisitIdWithDetails(visit.getVisitId());
        java.util.List<String> services = examinations.stream()
                .map(record -> record.getQueueTicket().getService() != null
                        ? record.getQueueTicket().getService().getName() : "Khám bệnh")
                .filter(java.util.Objects::nonNull).distinct().toList();
        java.util.List<String> doctors = examinations.stream()
                .map(record -> record.getDoctor() != null && record.getDoctor().getProfile() != null
                        ? record.getDoctor().getProfile().getFullName() : null)
                .filter(name -> name != null && !name.isBlank()).distinct().toList();
        String diagnoses = examinations.stream()
                .map(MedicalRecord::getDiagnosis)
                .filter(value -> value != null && !value.isBlank())
                .distinct().collect(java.util.stream.Collectors.joining("; "));
        int signedTestCount = (int) requests.stream()
                .filter(request -> request.getStatus() == TestRequestStatus.COMPLETED)
                .filter(request -> request.getTestResult() != null)
                .filter(request -> testResultRevisionRepo
                        .findFirstByTestResult_ResultIdAndStatusOrderByRevisionNoDesc(
                                request.getTestResult().getResultId(),
                                org.example.doansummer2026.enums.TestResultRevisionStatus.SIGNED)
                        .isPresent())
                .count();
        java.util.List<String> signedTestNames = requests.stream()
                .filter(request -> request.getStatus() == TestRequestStatus.COMPLETED)
                .filter(request -> request.getTestResult() != null)
                .filter(request -> testResultRevisionRepo
                        .findFirstByTestResult_ResultIdAndStatusOrderByRevisionNoDesc(
                                request.getTestResult().getResultId(),
                                org.example.doansummer2026.enums.TestResultRevisionStatus.SIGNED)
                        .isPresent())
                .map(request -> request.getService() != null ? request.getService().getName() : null)
                .filter(java.util.Objects::nonNull).distinct().toList();
        java.util.List<String> completedServiceNames = java.util.stream.Stream
                .concat(services.stream(), signedTestNames.stream()).distinct().toList();
        java.util.List<org.example.doansummer2026.model.QueueTicket> visitQueues = queueTicketRepo
                .findAllByVisit_VisitId(visit.getVisitId());
        java.util.Set<UUID> completedRecordTicketIds = examinations.stream()
                .map(MedicalRecord::getQueueTicket).filter(java.util.Objects::nonNull)
                .map(org.example.doansummer2026.model.QueueTicket::getTicketId)
                .collect(java.util.stream.Collectors.toSet());
        visitQueues.stream().filter(ticket -> ticket.getStatus() == QueueStatus.DONE)
                .filter(ticket -> !completedRecordTicketIds.contains(ticket.getTicketId()))
                .forEach(ticket -> log.warn(
                        "Queue ticket {} of visit {} is DONE but has no published medical record",
                        ticket.getTicketId(), visit.getVisitId()));
        java.util.List<String> skippedServiceNames = visitQueues.stream()
                .filter(ticket -> ticket.getStatus() == QueueStatus.SKIPPED)
                .map(ticket -> ticket.getService() != null ? ticket.getService().getName() : null)
                .filter(java.util.Objects::nonNull).distinct().toList();
        String completionStatus = !skippedServiceNames.isEmpty()
                || visit.getStatus() == org.example.doansummer2026.enums.VisitStatus.CANCELLED
                ? "PARTIAL" : "COMPLETE";
        LocalDateTime checkedIn = visit.getCheckInTime();
        return new VisitHistorySummaryResponse(
                visit.getVisitId(), visit.getVisitId(),
                "VIS-" + visit.getVisitId().toString().substring(0, 8).toUpperCase(),
                checkedIn != null ? checkedIn.toLocalDate().toString() : null,
                checkedIn != null ? checkedIn.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) : null,
                visit.getStatus() != null ? visit.getStatus().name() : null,
                services, examinations.size(), signedTestCount, doctors,
                diagnoses.isBlank() ? null : diagnoses,
                completionStatus, completedServiceNames, skippedServiceNames,
                skippedServiceNames.size());
    }

    /** Chi tiet customer theo visitId; chi dua noi dung chuyen mon da hoan thanh ra ngoai. */
    @Transactional(readOnly = true)
    public org.example.doansummer2026.dto.medicalhistory.VisitDetailResponse getPatientVisitDetail(
            UUID visitId, UUID profileId) {
        CustomerVisit visit = visitRepo.findById(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt khám"));
        if (visit.getCustomer() == null || !visit.getCustomer().getProfileId().equals(profileId)) {
            throw new ResourceNotFoundException("Không tìm thấy lượt khám");
        }
        java.util.List<MedicalRecord> allRecords = repo
                .findAllByVisit_VisitIdOrderByCreatedAtAsc(visitId);
        var requests = testRequestRepo.findAllByVisitIdWithDetails(visitId).stream()
                .filter(request -> request.getStatus() == TestRequestStatus.COMPLETED)
                .filter(request -> request.getTestResult() != null)
                .filter(request -> testResultRevisionRepo
                        .findFirstByTestResult_ResultIdAndStatusOrderByRevisionNoDesc(
                                request.getTestResult().getResultId(),
                                org.example.doansummer2026.enums.TestResultRevisionStatus.SIGNED)
                        .isPresent())
                .toList();
        boolean hasCompletedExamination = allRecords.stream()
                .anyMatch(record -> record.getStatus() == MedicalRecordStatus.COMPLETED
                        && record.getQueueTicket() != null
                        && record.getQueueTicket().getDepartment() != null
                        && record.getQueueTicket().getDepartment().getDepartmentType()
                        == org.example.doansummer2026.enums.DepartmentType.EXAMINATION);
        if (!hasCompletedExamination && requests.isEmpty()) {
            throw new ResourceNotFoundException("Lượt khám chưa có bệnh án hoặc kết quả đã hoàn thành");
        }
        if (allRecords.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy hồ sơ gắn với lượt khám");
        }
        return org.example.doansummer2026.dto.medicalhistory.VisitDetailResponse.publishedHistory(
                allRecords, requests, signedResultAttachments(requests),
                sameDayParaclinicalResultService.findForVisit(visitId),
                queueTicketRepo.findAllByVisit_VisitId(visitId));
    }

    /** Lich su de bac si tham khao trong luc dang kham; khong tra lai ho so hien tai. */
    @Transactional(readOnly = true)
    public java.util.List<MedicalHistoryResponse> getPreviousHistoryForDoctor(UUID currentRecordId) {
        MedicalRecord current = findById(currentRecordId);
        if (current.getVisit() == null || current.getVisit().getCustomer() == null) {
            return java.util.List.of();
        }
        return repo.findCompletedHistoryByProfileIdExcludingRecord(
                        current.getVisit().getCustomer().getProfileId(), currentRecordId)
                .stream()
                .map(MedicalHistoryResponse::from)
                .toList();
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
            // Ho so tam dung de gan CLS khong phai la benh an kham benh.
            // Lich su customer chi hien moi dich vu kham thanh mot muc rieng.
            predicates.add(cb.isNotNull(root.get("queueTicket")));
            predicates.add(cb.equal(
                    root.get("queueTicket").get("department").get("departmentType"),
                    org.example.doansummer2026.enums.DepartmentType.EXAMINATION));

            if (search != null && !search.isBlank()) {
                String searchLower = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("recordCode")), searchLower),
                        cb.like(cb.lower(root.get("diagnosis")), searchLower),
                        cb.like(cb.lower(root.get("queueTicket").get("service").get("name")), searchLower),
                        cb.like(cb.lower(root.get("doctor").get("profile").get("fullName")), searchLower)
                ));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    /**
     * Chi tiet luot kham cua benh nhan (theo visitId).
     */
    @Transactional(readOnly = true)
    public org.example.doansummer2026.dto.medicalhistory.VisitDetailResponse getVisitDetail(UUID visitId, UUID profileId) {
        org.example.doansummer2026.model.MedicalRecord record = repo.findFirstByVisit_VisitIdOrderByCreatedAtDesc(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Hồ sơ không tồn tại: " + visitId));

        // Kiem tra quyen so huu
        if (record.getVisit() == null || record.getVisit().getCustomer() == null
                || !record.getVisit().getCustomer().getProfileId().equals(profileId)) {
            throw new ResourceNotFoundException("Không tìm thấy hồ sơ");
        }

        UUID resolvedVisitId = record.getVisit().getVisitId();
        var requests = testRequestRepo.findAllByVisitIdWithDetails(resolvedVisitId);
        return org.example.doansummer2026.dto.medicalhistory.VisitDetailResponse.from(
                repo.findAllByVisit_VisitIdOrderByCreatedAtAsc(resolvedVisitId), requests,
                signedResultAttachments(requests), sameDayParaclinicalResultService.findForVisit(resolvedVisitId));
    }

    /**
     * Chi tiet luot kham cua benh nhan (theo recordId).
     */
    @Transactional(readOnly = true)
    public org.example.doansummer2026.dto.medicalhistory.VisitDetailResponse getVisitDetailByRecordId(UUID recordId, UUID profileId) {
        org.example.doansummer2026.model.MedicalRecord record = repo.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Hồ sơ không tồn tại: " + recordId));

        // Kiem tra quyen so huu
        if (record.getVisit() == null || record.getVisit().getCustomer() == null
                || !record.getVisit().getCustomer().getProfileId().equals(profileId)) {
            throw new ResourceNotFoundException("Không tìm thấy hồ sơ");
        }

        UUID resolvedVisitId = record.getVisit().getVisitId();
        var requests = testRequestRepo.findAllByVisitIdWithDetails(resolvedVisitId);
        // Record dang duoc mo phai la noi dung chinh cua trang. Cac benh an
        // khac trong cung VIS chi duoc dung cho danh sach chuyen nhanh.
        java.util.List<MedicalRecord> orderedRecords = new java.util.ArrayList<>(
                repo.findAllByVisit_VisitIdOrderByCreatedAtAsc(resolvedVisitId));
        orderedRecords.sort(java.util.Comparator.comparingInt(
                candidate -> candidate.getRecordId().equals(recordId) ? 0 : 1));
        return org.example.doansummer2026.dto.medicalhistory.VisitDetailResponse.from(
                orderedRecords, requests,
                signedResultAttachments(requests), sameDayParaclinicalResultService.findForVisit(resolvedVisitId));
    }

    @Transactional(readOnly = true)
    public org.example.doansummer2026.dto.medicalhistory.VisitDetailResponse getVisitDetailForStaff(UUID recordId) {
        MedicalRecord record = findById(recordId);
        if (record.getVisit() == null) throw new ResourceNotFoundException("Hồ sơ chưa gắn lượt khám");
        UUID visitId = record.getVisit().getVisitId();
        var requests = testRequestRepo.findAllByVisitIdWithDetails(visitId);
        return org.example.doansummer2026.dto.medicalhistory.VisitDetailResponse.from(
                repo.findAllByVisit_VisitIdOrderByCreatedAtAsc(visitId), requests,
                signedResultAttachments(requests), sameDayParaclinicalResultService.findForVisit(visitId));
    }

    private java.util.Map<UUID, java.util.List<org.example.doansummer2026.dto.testresult.TestResultAttachmentResponse>>
    signedResultAttachments(java.util.List<org.example.doansummer2026.model.TestRequest> requests) {
        java.util.Map<UUID, java.util.List<org.example.doansummer2026.dto.testresult.TestResultAttachmentResponse>> result
                = new java.util.HashMap<>();
        for (var request : requests) {
            var testResult = request.getTestResult();
            if (testResult == null) continue;
            var signed = testResultRevisionRepo.findFirstByTestResult_ResultIdAndStatusOrderByRevisionNoDesc(
                    testResult.getResultId(), org.example.doansummer2026.enums.TestResultRevisionStatus.SIGNED);
            if (signed.isEmpty()) continue;
            result.put(testResult.getResultId(), testResultAttachmentRepo
                    .findByRevision_RevisionIdOrderByDisplayOrder(signed.get().getRevisionId()).stream()
                    .map(org.example.doansummer2026.dto.testresult.TestResultAttachmentResponse::from).toList());
        }
        return result;
    }

    /**
     * Danh gia phong kham (1-5 sao). Chi ap dung cho EXAMINATION da hoan thanh (COMPLETED).
     */
    public MedicalRecordResponse rate(UUID id, int ratingScore) {
        if (ratingScore < 1 || ratingScore > 5) {
            throw new BadRequestException("Điểm đánh giá phải từ 1 đến 5 sao");
        }
        MedicalRecord r = findById(id);
        if (r.getStatus() != MedicalRecordStatus.COMPLETED) {
            throw new BadRequestException("Chỉ có thể đánh giá phiếu đã hoàn thành; trạng thái hiện tại: " + r.getStatus());
        }
        r.setRatingScore(ratingScore);
        r.setRatedAt(LocalDateTime.now());
        return MedicalRecordResponse.from(repo.save(r), true);
    }

    public org.example.doansummer2026.dto.medicalrecord.FeedbackResponse submitFeedback(
            UUID id, UUID profileId, org.example.doansummer2026.dto.medicalrecord.FeedbackRequest req) {
        MedicalRecord r = findById(id);
        if (r.getVisit() == null || r.getVisit().getCustomer() == null
                || !r.getVisit().getCustomer().getProfileId().equals(profileId)) {
            throw new ResourceNotFoundException("Không tìm thấy hồ sơ");
        }
        if (r.getStatus() != MedicalRecordStatus.COMPLETED) throw new BadRequestException("Chỉ có thể đánh giá dịch vụ đã hoàn thành");
        r.setRatingScore(req.overallRating());
        r.setDoctorRating(null); r.setWaitingRating(null); r.setStaffRating(null);
        r.setRatingComment(req.comment()); r.setContactRequested(false);
        r.setRatedAt(LocalDateTime.now()); r.setFeedbackStatus("NEW");
        r.getFeedbackTargets().clear();
        return org.example.doansummer2026.dto.medicalrecord.FeedbackResponse.from(repo.save(r));
    }

    @Transactional(readOnly = true)
    public PageResponse<org.example.doansummer2026.dto.medicalrecord.FeedbackResponse> listFeedbacks(UUID doctorId, Pageable pageable) {
        var page = doctorId == null ? repo.findByRatingScoreIsNotNull(pageable)
                : repo.findFeedbacksForStaff(doctorId, pageable);
        return PageResponse.from(page, org.example.doansummer2026.dto.medicalrecord.FeedbackResponse::from);
    }

    @Transactional(readOnly = true)
    public long countUnansweredFeedbacks() {
        return repo.countUnansweredFeedbacks();
    }

    public org.example.doansummer2026.dto.medicalrecord.FeedbackResponse respondFeedback(
            UUID id, UUID staffId, String response) {
        MedicalRecord r = findById(id);
        if (response != null) { r.setManagerResponse(response); r.setRespondedAt(LocalDateTime.now());
            if (staffId != null) r.setRespondedBy(staffRepo.findById(staffId).orElse(null)); }
        r.setFeedbackStatus(response != null ? "RESPONDED" : "IN_REVIEW");
        return org.example.doansummer2026.dto.medicalrecord.FeedbackResponse.from(repo.save(r));
    }

    public org.example.doansummer2026.dto.medicalrecord.FeedbackResponse explainFeedback(UUID id, UUID doctorId, String explanation) {
        MedicalRecord r = findById(id);
        boolean related = r.getDoctor() != null && r.getDoctor().getStaffId().equals(doctorId)
                || r.getFeedbackTargets().stream().anyMatch(t -> t.getStaff() != null && t.getStaff().getStaffId().equals(doctorId));
        if (!related) throw new ResourceNotFoundException("Đánh giá không thuộc bác sĩ này");
        r.setDoctorExplanation(explanation); r.setFeedbackStatus("WAITING_INTERNAL");
        return org.example.doansummer2026.dto.medicalrecord.FeedbackResponse.from(repo.save(r));
    }

    /** Tạo lịch hẹn từ yêu cầu tái khám và cập nhật vào hồ sơ bệnh án */
    public org.example.doansummer2026.dto.medicalrecord.FollowUpResponse scheduleFollowUp(UUID recordId, org.example.doansummer2026.dto.appointment.AppointmentCreateRequest req) {
        MedicalRecord record = findById(recordId);
        if (record.getFollowUpAppointment() != null) {
            throw new ConflictException("Yêu cầu tái khám này đã được đặt lịch hẹn");
        }

        // Tạo Appointment trực tiếp thay vì qua AppointmentService.create để hỗ trợ cả Guest và Profile không có Account
        var originalVisit = record.getVisit();
        var customer = originalVisit.getCustomer();
        var oldAppt = originalVisit.getAppointment();

        org.example.doansummer2026.model.ShiftConfig shift = req.shiftId() != null 
                ? shiftConfigRepository.findById(req.shiftId())
                    .orElseThrow(() -> new org.example.doansummer2026.exception.ResourceNotFoundException("Ca khám không tồn tại"))
                : null;
        ShiftScheduleResolver.ResolvedShift resolvedShift = null;
        if (shift != null) {
            if (req.scheduledAt() == null) {
                throw new BadRequestException("Vui lòng chọn ngày tái khám");
            }
            resolvedShift = shiftScheduleResolver.resolve(shift, req.scheduledAt().toLocalDate());
            if (!resolvedShift.available()) {
                throw new ConflictException("Ca tái khám không khả dụng: " + resolvedShift.unavailableReason());
            }
            var requestedTime = req.scheduledAt().toLocalTime();
            if (requestedTime.isBefore(resolvedShift.startTime()) || !requestedTime.isBefore(resolvedShift.endTime())) {
                throw new BadRequestException("Giờ tái khám không thuộc khung giờ thực tế của ca đã chọn");
            }
        }

        org.example.doansummer2026.model.Appointment appointment = org.example.doansummer2026.model.Appointment.builder()
                .scheduledAt(req.scheduledAt())
                .shiftName(shift != null ? shift.getName() : null)
                .shiftTime(resolvedShift != null
                        ? resolvedShift.startTime() + " - " + resolvedShift.endTime() : null)
                .shiftVersion(resolvedShift != null ? resolvedShift.version() : null)
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

        // Tái khám quay lại đúng dịch vụ/phòng khám đã đưa ra chỉ định.
        // Không tạo lượt khám hay hàng chờ tại thời điểm lễ tân đặt lịch.
        if (req.serviceIds() != null && !req.serviceIds().isEmpty()) {
            for (UUID serviceId : req.serviceIds()) {
                var service = medicalServiceRepo.findById(serviceId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Dịch vụ không tồn tại: " + serviceId
                                )
                        );

                appointment.getServices().add(service);
            }
        } else if (
                record.getQueueTicket() != null
                        && record.getQueueTicket().getService() != null
        ) {
            appointment.getServices().add(
                    record.getQueueTicket().getService()
            );
        }

        appointment = appointmentRepo.save(appointment);
        
        // Lưu đầy đủ trạng thái yêu cầu để lần tải sau vẫn nhận biết lịch tái khám đã được đặt.
        record.setFollowUpAppointment(appointment);
        record.setFollowUpDate(req.scheduledAt().toLocalDate());
        
        return org.example.doansummer2026.dto.medicalrecord.FollowUpResponse.from(repo.saveAndFlush(record));
    }
}
