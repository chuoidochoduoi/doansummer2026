package vn.edu.fpt.cares.service.interfaces;

import vn.edu.fpt.cares.dto.scheduletemplate.ScheduleTemplateResponse;
import vn.edu.fpt.cares.dto.scheduletemplate.ScheduleTemplateRequest;
import vn.edu.fpt.cares.model.StaffScheduleTemplate;

import java.util.List;
import java.util.UUID;

/** Service interface for StaffScheduleTemplate management. */
public interface StaffScheduleTemplateServiceInterface {
    ScheduleTemplateResponse create(ScheduleTemplateRequest req);
    ScheduleTemplateResponse update(UUID id, ScheduleTemplateRequest req);
    void delete(UUID id);
    List<ScheduleTemplateResponse> listByStaff(UUID staffId);
    ScheduleTemplateResponse get(UUID id);
    StaffScheduleTemplate findById(UUID id);
}



