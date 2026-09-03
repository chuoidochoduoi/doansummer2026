package org.example.doansummer2026.service.interfaces;

import org.example.doansummer2026.dto.scheduletemplate.ScheduleTemplateResponse;
import org.example.doansummer2026.dto.scheduletemplate.ScheduleTemplateRequest;
import org.example.doansummer2026.model.StaffScheduleTemplate;

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



