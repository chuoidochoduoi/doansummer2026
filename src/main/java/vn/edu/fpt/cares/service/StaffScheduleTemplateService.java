package vn.edu.fpt.cares.service;

import lombok.RequiredArgsConstructor;
import vn.edu.fpt.cares.dto.scheduletemplate.ScheduleTemplateRequest;
import vn.edu.fpt.cares.dto.scheduletemplate.ScheduleTemplateResponse;
import vn.edu.fpt.cares.exception.ConflictException;
import vn.edu.fpt.cares.exception.ResourceNotFoundException;
import vn.edu.fpt.cares.model.ShiftConfig;
import vn.edu.fpt.cares.model.StaffInfo;
import vn.edu.fpt.cares.model.StaffScheduleTemplate;
import vn.edu.fpt.cares.repository.ShiftConfigRepository;
import vn.edu.fpt.cares.repository.StaffScheduleTemplateRepository;
import vn.edu.fpt.cares.repository.StaffScheduleRepository;
import org.springframework.stereotype.Service;
import vn.edu.fpt.cares.service.interfaces.StaffScheduleTemplateServiceInterface;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

/**
 * Lich mau theo thu trong tuan (1 nhan vien <= 1 ca/ngay trong template).
 * Dung de StaffScheduleService sinh ra StaffSchedule cu the cho 1 tuan.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class StaffScheduleTemplateService implements StaffScheduleTemplateServiceInterface {

    private final StaffScheduleTemplateRepository repo;
    private final StaffScheduleRepository staffScheduleRepository;
    private final ShiftConfigRepository shiftConfigRepo;
    private final StaffService staffService;

    public ScheduleTemplateResponse create(ScheduleTemplateRequest req) {
        StaffInfo staff = staffService.findById(req.staffId());
        validateUnique(staff, req.dayOfWeek(), null);
        ShiftConfig shift = shiftConfigRepo.findById(req.shiftId())
                .orElseThrow(() -> new ResourceNotFoundException("Ca làm việc không tồn tại: " + req.shiftId()));
        if (Boolean.FALSE.equals(shift.getIsActive())) {
            throw new ConflictException("Ca làm việc đã ngừng hoạt động và không thể dùng cho mẫu lịch mới");
        }
        StaffScheduleTemplate t = StaffScheduleTemplate.builder()
                .staff(staff)
                .dayOfWeek(req.dayOfWeek())
                .shift(shift)
                .isActive(req.isActive() == null ? Boolean.TRUE : req.isActive())
                .build();
        return ScheduleTemplateResponse.from(repo.save(t));
    }

    public ScheduleTemplateResponse update(UUID id, ScheduleTemplateRequest req) {
        StaffScheduleTemplate t = findById(id);
        StaffInfo staff = req.staffId() != null
                ? staffService.findById(req.staffId()) : t.getStaff();
        DayOfWeek dow = req.dayOfWeek() != null ? req.dayOfWeek() : t.getDayOfWeek();
        validateUnique(staff, dow, id);
        t.setStaff(staff);
        t.setDayOfWeek(dow);
        if (req.shiftId() != null) {
            ShiftConfig shift = shiftConfigRepo.findById(req.shiftId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ca làm việc không tồn tại: " + req.shiftId()));
            if (Boolean.FALSE.equals(shift.getIsActive())) {
                throw new ConflictException("Ca làm việc đã ngừng hoạt động và không thể gán cho mẫu lịch");
            }
            t.setShift(shift);
        }
        if (req.isActive() != null) t.setIsActive(req.isActive());
        return ScheduleTemplateResponse.from(repo.save(t));
    }

    public void delete(UUID id) {
        StaffScheduleTemplate template = findById(id);
        if (staffScheduleRepository.countByTemplate_TemplateId(id) > 0) {
            template.setIsActive(false);
            repo.save(template);
            return;
        }
        repo.delete(template);
    }

    @Transactional(readOnly = true)
    public List<ScheduleTemplateResponse> listByStaff(UUID staffId) {
        StaffInfo staff = staffService.findById(staffId);
        return repo.findByStaff(staff).stream().map(ScheduleTemplateResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ScheduleTemplateResponse get(UUID id) {
        return ScheduleTemplateResponse.from(findById(id));
    }

    public StaffScheduleTemplate findById(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mẫu lịch làm việc không tồn tại: " + id));
    }

    private void validateUnique(StaffInfo staff, DayOfWeek dow, UUID ignoreId) {
        repo.findByStaffAndDayOfWeek(staff, dow).ifPresent(existing -> {
            if (ignoreId == null || !existing.getTemplateId().equals(ignoreId)) {
                throw new ConflictException(
                        "Đã tồn tại mẫu lịch cho nhân viên " + staff.getStaffCode() + " vào " + dow);
            }
        });
    }
}



