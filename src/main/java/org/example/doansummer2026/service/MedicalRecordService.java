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
import org.example.doansummer2026.repository.MedicalServiceRepository;
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
    private final MedicalServiceRepository medicalServiceRepo;
    private final org.example.doansummer2026.repository.ShiftConfigRepository shiftConfigRepository;
    private final NotificationService notificationService;
    private final AuthService authService;

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

        // Tim trong Profile (chi lay CUSTOMER, khong lay STAFF)
        profileRepo.findFirstByPhone(phone).ifPresent(p -> {
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
            var guestAppointments = appointmentRepo.findGuestAppointmentsByPhone(phone);
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

            // Search theo ten, phone
            if (search != null && !search.isEmpty()) {
                String searchLower = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), searchLower),
                        cb.like(cb.lower(root.get("phone")), searchLower),
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
        if (r.getStatus() == MedicalRecordStatus.COMPLETED) {
            throw new ConflictException("Không thể cập nhật hồ sơ đã hoàn thành");
        }
        validateVersion(r, req);
        ensureDoctorCanEdit(r);
        updateMedicalRecordFields(r, req);
        return MedicalRecordResponse.from(repo.save(r), true);
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
        boolean nurse = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_NURSE"));
        var actor = currentStaff().orElse(null);
        if (!isCurrentAdmin() && actor == null) {
            throw new BadRequestException("Không xác định được nhân viên đang thao tác");
        }
        if (nurse && r.getQueueTicket() == null) {
            throw new BadRequestException("Y tá chỉ được lưu hồ sơ tại phòng đang được phân công");
        }
        if (nurse && actor != null && r.getQueueTicket() != null
                && (actor.getDepartment() == null || !actor.getDepartment().getDepartmentId().equals(r.getQueueTicket().getDepartment().getDepartmentId())))
            throw new BadRequestException("Y tá chỉ được cập nhật hồ sơ tại phòng được phân công");
        if (!nurse) ensureDoctorCanEdit(r);
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
            throw new ConflictException("Hồ sơ đã được nhân viên khác cập nhật. Vui lòng tải lại dữ liệu trước khi lưu");
    }

    private java.util.Optional<StaffInfo> currentStaff() {
        UUID staffId = authService.currentStaffId();
        return staffId == null ? java.util.Optional.empty() : staffRepo.findById(staffId);
    }

    private boolean isCurrentAdmin() {
        return authService.getCurrentSystemRole() == org.example.doansummer2026.enums.SystemRole.ADMIN;
    }

    private void ensureDoctorCanEdit(MedicalRecord record) {
        if (isCurrentAdmin()) return;
        StaffInfo actor = currentStaff()
                .orElseThrow(() -> new BadRequestException("Không xác định được bác sĩ đang thao tác"));
        if (!actor.getSystemRole().isDoctor()) {
            throw new BadRequestException("Chỉ bác sĩ phụ trách mới được cập nhật hồ sơ khám");
        }
        UUID responsibleDoctorId = record.getQueueTicket() != null
                && record.getQueueTicket().getDepartment() != null
                && record.getQueueTicket().getDepartment().getHeadDoctor() != null
                ? record.getQueueTicket().getDepartment().getHeadDoctor().getStaffId()
                : (record.getDoctor() != null ? record.getDoctor().getStaffId() : null);
        if (responsibleDoctorId == null || !responsibleDoctorId.equals(actor.getStaffId())) {
            throw new BadRequestException("Ca khám này thuộc bác sĩ phụ trách khác");
        }
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
        if (!isCurrentAdmin() && actor == null) {
            throw new BadRequestException("Không xác định được bác sĩ đang thao tác");
        }
        // Bac si phu trach duoc cau hinh cho phong co quyen ket thuc ca kham.
        // Bac si luu trong record co the la nguoi tao benh an ban dau, nen chi
        // dung lam du phong khi queue/phong chua duoc cau hinh bac si phu trach.
        UUID responsibleDoctorId = r.getQueueTicket() != null
                && r.getQueueTicket().getDepartment() != null
                && r.getQueueTicket().getDepartment().getHeadDoctor() != null
                ? r.getQueueTicket().getDepartment().getHeadDoctor().getStaffId()
                : (r.getDoctor() != null ? r.getDoctor().getStaffId() : null);
        if (!isCurrentAdmin()) {
            if (!actor.getSystemRole().isDoctor()
                    || responsibleDoctorId == null
                    || !responsibleDoctorId.equals(actor.getStaffId())) {
                throw new BadRequestException("Chỉ bác sĩ phụ trách mới được hoàn thành ca khám");
            }
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
        var page = repo.findAll(spec, pageable);

        return PageResponse.from(page, MedicalHistoryResponse::from);
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
                .orElseThrow(() -> new ResourceNotFoundException("Hồ sơ không tồn tại: " + visitId));

        // Kiem tra quyen so huu
        if (record.getVisit() == null || record.getVisit().getCustomer() == null
                || !record.getVisit().getCustomer().getProfileId().equals(profileId)) {
            throw new ResourceNotFoundException("Không tìm thấy hồ sơ");
        }

        UUID resolvedVisitId = record.getVisit().getVisitId();
        return org.example.doansummer2026.dto.medicalHistory.VisitDetailResponse.from(
                repo.findAllByVisit_VisitIdOrderByCreatedAtAsc(resolvedVisitId),
                testRequestRepo.findAllByVisitIdWithDetails(resolvedVisitId));
    }

    /**
     * Chi tiet luot kham cua benh nhan (theo recordId).
     */
    @Transactional(readOnly = true)
    public org.example.doansummer2026.dto.medicalHistory.VisitDetailResponse getVisitDetailByRecordId(UUID recordId, UUID profileId) {
        org.example.doansummer2026.model.MedicalRecord record = repo.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Hồ sơ không tồn tại: " + recordId));

        // Kiem tra quyen so huu
        if (record.getVisit() == null || record.getVisit().getCustomer() == null
                || !record.getVisit().getCustomer().getProfileId().equals(profileId)) {
            throw new ResourceNotFoundException("Không tìm thấy hồ sơ");
        }

        UUID resolvedVisitId = record.getVisit().getVisitId();
        return org.example.doansummer2026.dto.medicalHistory.VisitDetailResponse.from(
                repo.findAllByVisit_VisitIdOrderByCreatedAtAsc(resolvedVisitId),
                testRequestRepo.findAllByVisitIdWithDetails(resolvedVisitId));
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

    public org.example.doansummer2026.dto.medicalRecord.FeedbackResponse submitFeedback(
            UUID id, UUID profileId, org.example.doansummer2026.dto.medicalRecord.FeedbackRequest req) {
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
        return org.example.doansummer2026.dto.medicalRecord.FeedbackResponse.from(repo.save(r));
    }

    @Transactional(readOnly = true)
    public PageResponse<org.example.doansummer2026.dto.medicalRecord.FeedbackResponse> listFeedbacks(UUID doctorId, Pageable pageable) {
        var page = doctorId == null ? repo.findByRatingScoreIsNotNull(pageable)
                : repo.findFeedbacksForStaff(doctorId, pageable);
        return PageResponse.from(page, org.example.doansummer2026.dto.medicalRecord.FeedbackResponse::from);
    }

    public org.example.doansummer2026.dto.medicalRecord.FeedbackResponse respondFeedback(
            UUID id, UUID staffId, String response) {
        MedicalRecord r = findById(id);
        if (response != null) { r.setManagerResponse(response); r.setRespondedAt(LocalDateTime.now());
            if (staffId != null) r.setRespondedBy(staffRepo.findById(staffId).orElse(null)); }
        r.setFeedbackStatus(response != null ? "RESPONDED" : "IN_REVIEW");
        return org.example.doansummer2026.dto.medicalRecord.FeedbackResponse.from(repo.save(r));
    }

    public org.example.doansummer2026.dto.medicalRecord.FeedbackResponse explainFeedback(UUID id, UUID doctorId, String explanation) {
        MedicalRecord r = findById(id);
        boolean related = r.getDoctor() != null && r.getDoctor().getStaffId().equals(doctorId)
                || r.getFeedbackTargets().stream().anyMatch(t -> t.getStaff() != null && t.getStaff().getStaffId().equals(doctorId));
        if (!related) throw new ResourceNotFoundException("Đánh giá không thuộc bác sĩ này");
        r.setDoctorExplanation(explanation); r.setFeedbackStatus("WAITING_INTERNAL");
        return org.example.doansummer2026.dto.medicalRecord.FeedbackResponse.from(repo.save(r));
    }

    /** Lấy danh sách yêu cầu tái khám (follow-up) chưa đặt lịch cho lễ tân */
    public PageResponse<org.example.doansummer2026.dto.medicalRecord.FollowUpResponse> getPendingFollowUps(String search, Pageable pageable) {
        String normalizedSearch = search == null ? "" : search.trim().toLowerCase(java.util.Locale.ROOT);
        Page<MedicalRecord> page = repo.findPendingFollowUps(normalizedSearch, pageable);
        return PageResponse.from(page, org.example.doansummer2026.dto.medicalRecord.FollowUpResponse::from);
    }

    /** Tạo lịch hẹn từ yêu cầu tái khám và cập nhật vào hồ sơ bệnh án */
    public org.example.doansummer2026.dto.medicalRecord.FollowUpResponse scheduleFollowUp(UUID recordId, org.example.doansummer2026.dto.appointment.AppointmentCreateRequest req) {
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

        org.example.doansummer2026.model.Appointment appointment = org.example.doansummer2026.model.Appointment.builder()
                .scheduledAt(req.scheduledAt())
                .shiftName(shift != null ? shift.getName() : null)
                .shiftTime(shift != null ? shift.getStartTime() + " - " + shift.getEndTime() : null)
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
        
        // Cập nhật MedicalRecord với ID của lịch hẹn vừa tạo
        record.setFollowUpAppointment(appointment);
        
        return org.example.doansummer2026.dto.medicalRecord.FollowUpResponse.from(repo.save(record));
    }
}
