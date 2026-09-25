package vn.edu.fpt.cares.service;

import lombok.RequiredArgsConstructor;
import vn.edu.fpt.cares.common.PageResponse;
import vn.edu.fpt.cares.dto.department.DepartmentCreateRequest;
import vn.edu.fpt.cares.dto.department.DepartmentResponse;
import vn.edu.fpt.cares.dto.department.DepartmentUpdateRequest;
import vn.edu.fpt.cares.enums.DepartmentStatus;
import vn.edu.fpt.cares.enums.DepartmentType;
import vn.edu.fpt.cares.exception.ConflictException;
import vn.edu.fpt.cares.exception.BadRequestException;
import vn.edu.fpt.cares.exception.ResourceNotFoundException;
import vn.edu.fpt.cares.model.Department;
import vn.edu.fpt.cares.model.StaffInfo;
import vn.edu.fpt.cares.model.Specialization;
import vn.edu.fpt.cares.repository.DepartmentRepository;
import vn.edu.fpt.cares.repository.StaffInfoRepository;
import vn.edu.fpt.cares.repository.SpecializationRepository;
import vn.edu.fpt.cares.repository.ServiceCapabilityRepository;
import vn.edu.fpt.cares.repository.StaffScheduleRepository;
import vn.edu.fpt.cares.repository.MedicalRecordRepository;
import vn.edu.fpt.cares.enums.ScheduleStatus;
import vn.edu.fpt.cares.enums.MedicalRecordStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.edu.fpt.cares.service.interfaces.DepartmentServiceInterface;
import vn.edu.fpt.cares.service.AuthService;
import vn.edu.fpt.cares.model.Account;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@Transactional
@RequiredArgsConstructor
public class DepartmentService implements DepartmentServiceInterface {

    private final DepartmentRepository repo;
    private final StaffInfoRepository staffRepo;
    private final SpecializationRepository specializationRepo;
    private final ServiceCapabilityRepository capabilityRepo;
    private final StaffScheduleRepository staffScheduleRepo;
    private final MedicalRecordRepository medicalRecordRepo;
    private final StaffDutyService staffDutyService;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public PageResponse<DepartmentResponse> listAll(Pageable pageable) {
        Page<Department> page = repo.findAllWithHeadDoctor(pageable);
        return PageResponse.from(page, this::toResponse);
    }

    @Transactional(readOnly = true)
    public PageResponse<DepartmentResponse> list(DepartmentType departmentType, Pageable pageable) {
        Page<Department> page = repo.findAllByDepartmentType(departmentType, pageable);
        return PageResponse.from(page, this::toResponse);
    }

    @Transactional(readOnly = true)
    public PageResponse<DepartmentResponse> listMultiple(Pageable pageable, List<DepartmentType> departmentTypes) {
        Page<Department> page = repo.findAllByDepartmentTypeIn(departmentTypes, pageable);
        return PageResponse.from(page, this::toResponse);
    }

    @Transactional(readOnly = true)
    public DepartmentResponse get(UUID id) {
        return toResponse(findById(id));
    }

    public DepartmentResponse create(DepartmentCreateRequest req) {
        if (repo.existsByRoomCode(req.roomCode())) {
            throw new ConflictException("Mã phòng đã tồn tại: " + req.roomCode());
        }
        if (repo.existsByName(req.name())) {
            throw new ConflictException("Tên phòng đã tồn tại: " + req.name());
        }
        DepartmentType departmentType = req.departmentType() != null ? req.departmentType().normalized() : DepartmentType.EXAMINATION;
        if (departmentType == DepartmentType.EXAMINATION && req.specializationId() == null) {
            throw new BadRequestException("Vui lòng chọn chuyên khoa cho phòng khám");
        }
        Department.DepartmentBuilder builder = Department.builder()
                .roomCode(req.roomCode())
                .name(req.name())
                .status(req.status() != null ? req.status() : DepartmentStatus.AVAILABLE)
                .departmentType(departmentType)
                .description(req.description());

        if (departmentType == DepartmentType.EXAMINATION && req.specializationId() != null) {
            Specialization specialization = specializationRepo.findById(req.specializationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Chuyên khoa không tồn tại: " + req.specializationId()));
            if (Boolean.FALSE.equals(specialization.getActive())) {
                throw new ConflictException("Chuyên khoa đã ngừng hoạt động và không thể gán cho phòng mới");
            }
            builder.specialization(specialization);
        }

        if (req.headDoctorId() != null) {
            StaffInfo headDoctor = staffRepo.findById(req.headDoctorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Nhân viên không tồn tại: " + req.headDoctorId()));
            validateHeadDoctorRole(headDoctor);
            // Kiem tra bac si chua duoc gianh cho phong khac
            if (repo.existsByHeadDoctor_StaffId(req.headDoctorId())) {
                throw new ConflictException("Bác sĩ này đã phụ trách phòng khác: " + req.headDoctorId());
            }
            builder.headDoctor(headDoctor);
        }

        Department department = builder.build();
        if (req.capabilityIds() != null) {
            department.setCapabilities(new java.util.HashSet<>(capabilityRepo.findAllById(req.capabilityIds())));
            if (department.getCapabilities().size() != req.capabilityIds().stream().distinct().count()) {
                throw new ResourceNotFoundException("Danh mục kỹ thuật không tồn tại");
            }
            if (department.getCapabilities().stream().anyMatch(c -> Boolean.FALSE.equals(c.getActive()))) {
                throw new ConflictException("Không thể gán danh mục kỹ thuật đã ngừng hoạt động cho phòng");
            }
        }
        validateClinicalConfiguration(department);
        Department saved = repo.save(department);
        synchronizeClinicalStaff(saved, req.doctorIds(), req.nurseIds(), true);
        validateCurrentClinicalMembers(saved);

        // Fetch again to ensure nurses are loaded in response
        return toResponse(repo.findById(saved.getDepartmentId()).get());
    }

    public DepartmentResponse update(UUID id, DepartmentUpdateRequest req) {
        Department d = repo.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Phòng không tồn tại: " + id));
        validateStatusTransition(d, req.status());
        boolean changesClinicalClassification = req.departmentType() != null
                && req.departmentType().normalized() != d.getDepartmentType().normalized();
        if (!changesClinicalClassification && req.specializationId() != null) {
            UUID currentSpecializationId = d.getSpecialization() == null
                    ? null : d.getSpecialization().getSpecializationId();
            changesClinicalClassification = !java.util.Objects.equals(
                    currentSpecializationId, req.specializationId());
        }
        if (changesClinicalClassification && repo.countOperationalReferences(id) > 0) {
            throw new ConflictException(
                    "Không thể đổi loại phòng hoặc chuyên khoa sau khi phòng đã phát sinh dữ liệu khám");
        }
        if (req.roomCode() != null && !req.roomCode().equals(d.getRoomCode())) {
            if (repo.existsByRoomCode(req.roomCode())) {
                throw new ConflictException("Mã phòng đã tồn tại: " + req.roomCode());
            }
            d.setRoomCode(req.roomCode());
        }
        if (req.name() != null && !req.name().equals(d.getName())) {
            if (repo.existsByName(req.name())) {
                throw new ConflictException("Tên phòng đã tồn tại: " + req.name());
            }
            d.setName(req.name());
        }
        if (req.status() != null) d.setStatus(req.status());
        if (req.departmentType() != null) {
            d.setDepartmentType(req.departmentType().normalized());
            if (req.departmentType().normalized() != DepartmentType.EXAMINATION) {
                d.setSpecialization(null);
            }
        }
        if (d.getDepartmentType() == DepartmentType.EXAMINATION && req.specializationId() != null) {
            Specialization specialization = specializationRepo.findById(req.specializationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Chuyên khoa không tồn tại: " + req.specializationId()));
            if (Boolean.FALSE.equals(specialization.getActive())) {
                throw new ConflictException("Chuyên khoa đã ngừng hoạt động và không thể gán cho phòng");
            }
            d.setSpecialization(specialization);
        }
        if (req.capabilityIds() != null) {
            d.setCapabilities(new java.util.HashSet<>(capabilityRepo.findAllById(req.capabilityIds())));
            if (d.getCapabilities().size() != req.capabilityIds().stream().distinct().count()) {
                throw new ResourceNotFoundException("Danh mục kỹ thuật không tồn tại");
            }
            if (d.getCapabilities().stream().anyMatch(c -> Boolean.FALSE.equals(c.getActive()))) {
                throw new ConflictException("Không thể gán danh mục kỹ thuật đã ngừng hoạt động cho phòng");
            }
        }
        if (req.description() != null) d.setDescription(req.description());
        if (req.headDoctorId() != null) {
            // Kiem tra bac si moi chua duoc gianh cho phong khac (tru phong hien tai)
            if (repo.existsByHeadDoctor_StaffId(req.headDoctorId())
                    && (d.getHeadDoctor() == null || !d.getHeadDoctor().getStaffId().equals(req.headDoctorId()))) {
                throw new ConflictException("Bác sĩ này đã phụ trách phòng khác: " + req.headDoctorId());
            }
            StaffInfo headDoctor = staffRepo.findById(req.headDoctorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Nhân viên không tồn tại: " + req.headDoctorId()));
            validateHeadDoctorRole(headDoctor);
            d.setHeadDoctor(headDoctor);
        } else if (req.doctorIds() != null) {
            // Request cap nhat day du tu man quan ly phong: cho phep bo thong tin
            // phu trach chuyen mon ma khong anh huong danh sach bac si thuoc phong.
            d.setHeadDoctor(null);
        }

        validateClinicalConfiguration(d);
        Department saved = repo.save(d);
        synchronizeClinicalStaff(saved, req.doctorIds(), req.nurseIds(), false);
        validateCurrentClinicalMembers(saved);
        // Ensure nurses collection is up to date for the response mapping
        return toResponse(repo.findById(saved.getDepartmentId()).get());
    }

    public DepartmentResponse updateStatus(UUID id, DepartmentStatus status) {
        Department d = repo.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Phòng không tồn tại: " + id));
        if (status != null) {
            validateStatusTransition(d, status);
            d.setStatus(status);
        }
        return toResponse(repo.save(d));
    }

    private void synchronizeClinicalStaff(Department department, List<UUID> requestedDoctorIds,
                                          List<UUID> requestedNurseIds, boolean creating) {
        Set<UUID> doctorIds = requestedDoctorIds == null
                ? (creating ? new LinkedHashSet<>() : null)
                : new LinkedHashSet<>(requestedDoctorIds);
        if (department.getHeadDoctor() != null) {
            if (doctorIds == null) doctorIds = currentMemberIds(department, true);
            doctorIds.add(department.getHeadDoctor().getStaffId());
        }
        Set<UUID> nurseIds = requestedNurseIds == null
                ? (creating ? new LinkedHashSet<>() : null)
                : new LinkedHashSet<>(requestedNurseIds);

        List<StaffInfo> currentMembers = staffRepo.findByDepartment_DepartmentId(department.getDepartmentId());
        for (StaffInfo member : currentMembers) {
            boolean doctor = member.getSystemRole() != null && member.getSystemRole().isDoctor();
            boolean nurse = member.getSystemRole() == vn.edu.fpt.cares.enums.SystemRole.NURSE;
            boolean removed = doctor && doctorIds != null && !doctorIds.contains(member.getStaffId())
                    || nurse && nurseIds != null && !nurseIds.contains(member.getStaffId());
            if (removed) {
                ensureCanDetachFromDepartment(member);
                member.setDepartment(null);
                staffRepo.save(member);
            }
        }

        if (doctorIds != null) {
            for (UUID doctorId : doctorIds) {
                StaffInfo doctor = staffRepo.findById(doctorId)
                        .orElseThrow(() -> new ResourceNotFoundException("Bác sĩ không tồn tại: " + doctorId));
                validateHeadDoctorRole(doctor);
                assignMember(department, doctor, "Bác sĩ");
            }
        }
        if (nurseIds != null) {
            for (UUID nurseId : nurseIds) {
                StaffInfo nurse = staffRepo.findById(nurseId)
                        .orElseThrow(() -> new ResourceNotFoundException("Y tá không tồn tại: " + nurseId));
                if (nurse.getSystemRole() != vn.edu.fpt.cares.enums.SystemRole.NURSE) {
                    throw new BadRequestException("Nhân sự được chọn không phải là y tá: " + nurseId);
                }
                assignMember(department, nurse, "Y tá");
            }
        }
    }

    private Set<UUID> currentMemberIds(Department department, boolean doctors) {
        Set<UUID> ids = new LinkedHashSet<>();
        for (StaffInfo member : staffRepo.findByDepartment_DepartmentId(department.getDepartmentId())) {
            boolean matches = doctors
                    ? member.getSystemRole() != null && member.getSystemRole().isDoctor()
                    : member.getSystemRole() == vn.edu.fpt.cares.enums.SystemRole.NURSE;
            if (matches) ids.add(member.getStaffId());
        }
        return ids;
    }

    private void assignMember(Department department, StaffInfo staff, String label) {
        if (staff.getProfile() == null || staff.getProfile().getAccount() == null
                || !Boolean.TRUE.equals(staff.getProfile().getAccount().getIsActive())) {
            throw new ConflictException(label + " đã ngừng hoạt động và không thể thêm vào phòng");
        }
        if (staff.getDepartment() != null
                && !staff.getDepartment().getDepartmentId().equals(department.getDepartmentId())) {
            throw new ConflictException(label + " đã thuộc phòng khác. Hãy gỡ lịch và chuyển phòng trước: "
                    + staff.getStaffId());
        }
        staffDutyService.requireEligibility(staff, department);
        staff.setDepartment(department);
        staffRepo.save(staff);
    }

    private void ensureCanDetachFromDepartment(StaffInfo staff) {
        if (staffScheduleRepo.existsByStaff_StaffIdAndWorkDateGreaterThanEqualAndStatus(
                staff.getStaffId(), LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")), ScheduleStatus.SCHEDULED)) {
            throw new ConflictException("Không thể gỡ nhân sự khỏi phòng khi còn lịch trực hiện tại hoặc tương lai");
        }
        if (medicalRecordRepo.existsByDoctor_StaffIdAndStatusIn(staff.getStaffId(),
                List.of(MedicalRecordStatus.DRAFT, MedicalRecordStatus.IN_PROGRESS))) {
            throw new ConflictException("Không thể gỡ bác sĩ khỏi phòng khi còn bệnh án đang xử lý");
        }
    }

    private void validateHeadDoctorRole(StaffInfo staff) {
        if (staff.getSystemRole() == null || !staff.getSystemRole().isDoctor()) {
            throw new BadRequestException("Nhân viên phụ trách phòng phải là bác sĩ");
        }
        if (staff.getProfile() == null || staff.getProfile().getAccount() == null
                || !Boolean.TRUE.equals(staff.getProfile().getAccount().getIsActive())) {
            throw new ConflictException("Bác sĩ đã ngừng hoạt động và không thể phụ trách phòng");
        }
    }

    private void validateClinicalConfiguration(Department department) {
        DepartmentType type = department.getDepartmentType() == null
                ? DepartmentType.EXAMINATION : department.getDepartmentType().normalized();
        if (type == DepartmentType.EXAMINATION && department.getSpecialization() == null) {
            throw new BadRequestException("Vui lòng chọn chuyên khoa cho phòng khám");
        }
        if (type.isParaclinical()
                && (department.getCapabilities() == null || department.getCapabilities().isEmpty())) {
            throw new BadRequestException("Vui lòng chọn ít nhất một danh mục kỹ thuật cho phòng cận lâm sàng");
        }
    }

    private void validateCurrentClinicalMembers(Department department) {
        for (StaffInfo member : staffRepo.findByDepartment_DepartmentId(department.getDepartmentId())) {
            if (member.getSystemRole() != null && (member.getSystemRole().isDoctor()
                    || member.getSystemRole() == vn.edu.fpt.cares.enums.SystemRole.NURSE)) {
                staffDutyService.requireEligibility(member, department);
            }
        }
    }

    private void validateStatusTransition(Department department, DepartmentStatus requestedStatus) {
        if (requestedStatus != DepartmentStatus.MAINTENANCE
                || department.getStatus() == DepartmentStatus.MAINTENANCE) return;
        long openQueues = repo.countOpenQueueTickets(department.getDepartmentId());
        long openTestRequests = repo.countOpenTestRequests(department.getDepartmentId());
        if (openQueues > 0 || openTestRequests > 0) {
            throw new ConflictException(
                    "Không thể chuyển phòng sang bảo trì khi còn ca chưa hoàn thành ("
                            + openQueues + " hàng chờ, " + openTestRequests
                            + " yêu cầu cận lâm sàng). Hãy hoàn thành hoặc điều phối các ca trước.");
        }
        boolean hasCurrentOrFutureSchedule = staffScheduleRepo
                .existsByStaff_Department_DepartmentIdAndWorkDateGreaterThanEqualAndStatus(
                        department.getDepartmentId(),
                        LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")),
                        ScheduleStatus.SCHEDULED);
        if (hasCurrentOrFutureSchedule) {
            throw new ConflictException(
                    "Không thể chuyển phòng sang bảo trì khi còn lịch trực hiện tại hoặc tương lai. Hãy gỡ lịch trước.");
        }
    }

    public void delete(UUID id) {
        Department department = findById(id);
        long operationalReferences = repo.countOperationalReferences(id);
        if (operationalReferences > 0) {
            throw new ConflictException(
                    "Không thể xóa phòng đã phát sinh dữ liệu khám (" +
                    operationalReferences + " liên kết). Hãy chuyển phòng sang trạng thái bảo trì.");
        }

        long configurationReferences = repo.countActiveConfigurationReferences(id);
        if (configurationReferences > 0) {
            throw new ConflictException(
                    "Không thể xóa phòng đang được gán cho dịch vụ hoặc nhân sự (" +
                    configurationReferences + " liên kết). Hãy gỡ các cấu hình liên quan trước.");
        }

        // Xoa bang noi cua capability truoc khi soft-delete phong chua tung su dung.
        department.getCapabilities().clear();
        repo.saveAndFlush(department);
        repo.delete(department);
    }

    public Department findById(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Phòng không tồn tại: " + id));
    }

    @Transactional(readOnly = true)
    public Department findByIdWithHeadDoctor(UUID id) {
        return repo.findWithHeadDoctorById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Phòng không tồn tại: " + id));
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getMyDepartment() {
        Account acc = authService.currentAccount();
        StaffInfo staff = staffRepo.findFirstByProfile_Account_Username(acc.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Bạn không phải là nhân viên"));
        Optional<Department> dept = Optional.ofNullable(staff.getDepartment());
        return toResponse(
            dept.orElseThrow(() -> new ResourceNotFoundException("Chưa được chỉ định phòng"))
        );
    }

    private DepartmentResponse toResponse(Department department) {
        return DepartmentResponse.from(
                department,
                staffRepo.findByDepartment_DepartmentId(department.getDepartmentId()),
                staffDutyService.findOnDutyStaff(
                        department, LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))));
    }
}
