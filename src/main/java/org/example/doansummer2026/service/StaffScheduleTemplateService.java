package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.dto.scheduleTemplate.ScheduleTemplateRequest;
import org.example.doansummer2026.dto.scheduleTemplate.ScheduleTemplateResponse;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.model.StaffScheduleTemplate;
import org.example.doansummer2026.repository.StaffScheduleTemplateRepository;
import org.springframework.stereotype.Service;
import org.example.doansummer2026.service.interfaces.StaffScheduleTemplateServiceInterface;
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
    private final StaffService staffService;

    public ScheduleTemplateResponse create(ScheduleTemplateRequest req) {
        StaffInfo staff = staffService.findById(req.staffId());
        validateUnique(staff, req.dayOfWeek(), null);
        StaffScheduleTemplate t = StaffScheduleTemplate.builder()
                .staff(staff)
                .dayOfWeek(req.dayOfWeek())
                .shift(req.shift())
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
        if (req.shift() != null) t.setShift(req.shift());
        if (req.isActive() != null) t.setIsActive(req.isActive());
        return ScheduleTemplateResponse.from(repo.save(t));
    }

    public void delete(UUID id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Template khong ton tai: " + id);
        }
        repo.deleteById(id);
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
                .orElseThrow(() -> new ResourceNotFoundException("Template khong ton tai: " + id));
    }

    private void validateUnique(StaffInfo staff, DayOfWeek dow, UUID ignoreId) {
        repo.findByStaffAndDayOfWeek(staff, dow).ifPresent(existing -> {
            if (ignoreId == null || !existing.getTemplateId().equals(ignoreId)) {
                throw new ConflictException(
                        "Da ton tai template cho nhan vien " + staff.getStaffCode() + " vao " + dow);
            }
        });
    }
}