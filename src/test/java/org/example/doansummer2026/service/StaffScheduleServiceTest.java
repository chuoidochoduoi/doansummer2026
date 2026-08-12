package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.schedule.ScheduleAssignRequest;
import org.example.doansummer2026.dto.schedule.ScheduleCreateRequest;
import org.example.doansummer2026.dto.schedule.ScheduleUpdateRequest;
import org.example.doansummer2026.enums.ScheduleStatus;
import org.example.doansummer2026.enums.SystemRole;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.model.ShiftConfig;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.model.StaffSchedule;
import org.example.doansummer2026.model.StaffScheduleTemplate;
import org.example.doansummer2026.repository.ShiftConfigRepository;
import org.example.doansummer2026.repository.StaffScheduleRepository;
import org.example.doansummer2026.repository.StaffScheduleTemplateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffScheduleServiceTest {

    @Mock
    private StaffScheduleRepository scheduleRepo;

    @Mock
    private StaffScheduleTemplateRepository templateRepo;

    @Mock
    private ShiftConfigRepository shiftConfigRepo;

    @Mock
    private StaffService staffService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private StaffScheduleService service;


    // =========================================================
    // HELPERS
    // =========================================================

    private StaffInfo staff(UUID id, String fullName) {
        Profile profile = Profile.builder()
                .profileId(UUID.randomUUID())
                .fullName(fullName)
                .build();

        return StaffInfo.builder()
                .staffId(id)
                .staffCode("STF-" + id.toString().substring(0, 4))
                .profile(profile)
                .systemRole(SystemRole.NURSE)
                .build();
    }

    private ShiftConfig shift(UUID id, String name) {
        return ShiftConfig.builder()
                .shiftId(id)
                .name(name)
                .startTime("08:00")
                .endTime("12:00")
                .build();
    }

    private StaffSchedule schedule(
            UUID id,
            StaffInfo staff,
            LocalDate date,
            ShiftConfig shift
    ) {
        return StaffSchedule.builder()
                .scheduleId(id)
                .staff(staff)
                .workDate(date)
                .shift(shift)
                .status(ScheduleStatus.SCHEDULED)
                .isCustom(false)
                .build();
    }


    // =========================================================
    // CREATE
    // =========================================================

    @Test
    void create_ShouldThrow_WhenShiftMissing() {

        UUID staffId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();

        ScheduleCreateRequest req = mock(ScheduleCreateRequest.class);

        when(req.staffId()).thenReturn(staffId);
        when(req.shiftId()).thenReturn(shiftId);

        when(staffService.findById(staffId))
                .thenReturn(staff(staffId, "Nhan vien"));

        when(shiftConfigRepo.findById(shiftId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.create(req)
        );
    }


    @Test
    void create_ShouldUseDefaults_WhenStatusAndCustomAreNull() {

        UUID staffId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();

        StaffInfo staff = staff(staffId, "Nhan vien A");
        ShiftConfig shift = shift(shiftId, "Ca sang");

        ScheduleCreateRequest req = mock(ScheduleCreateRequest.class);

        when(req.staffId()).thenReturn(staffId);
        when(req.shiftId()).thenReturn(shiftId);
        when(req.workDate()).thenReturn(LocalDate.now());

        when(staffService.findById(staffId))
                .thenReturn(staff);

        when(shiftConfigRepo.findById(shiftId))
                .thenReturn(Optional.of(shift));

        when(scheduleRepo.save(any(StaffSchedule.class)))
                .thenAnswer(invocation -> {
                    StaffSchedule s = invocation.getArgument(0);
                    s.setScheduleId(scheduleId);
                    return s;
                });

        var result = service.create(req);

        assertNotNull(result);

        verify(scheduleRepo).save(argThat(s ->
                s.getStatus() == ScheduleStatus.SCHEDULED
                        && Boolean.FALSE.equals(s.getIsCustom())
                        && s.getStaff() == staff
                        && s.getShift() == shift
        ));
    }


    @Test
    void create_ShouldUseProvidedStatusAndCustom() {

        UUID staffId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();

        StaffInfo staff = staff(staffId, "Nhan vien");
        ShiftConfig shift = shift(shiftId, "Ca");

        ScheduleCreateRequest req = mock(ScheduleCreateRequest.class);

        when(req.staffId()).thenReturn(staffId);
        when(req.shiftId()).thenReturn(shiftId);
        when(req.workDate()).thenReturn(LocalDate.now());
        when(req.status()).thenReturn(ScheduleStatus.COMPLETED);
        when(req.isCustom()).thenReturn(true);
        when(req.note()).thenReturn("Ghi chu");

        when(staffService.findById(staffId))
                .thenReturn(staff);

        when(shiftConfigRepo.findById(shiftId))
                .thenReturn(Optional.of(shift));

        when(scheduleRepo.save(any()))
                .thenAnswer(i -> {
                    StaffSchedule s = i.getArgument(0);
                    s.setScheduleId(UUID.randomUUID());
                    return s;
                });

        service.create(req);

        verify(scheduleRepo).save(argThat(s ->
                s.getStatus() == ScheduleStatus.COMPLETED
                        && Boolean.TRUE.equals(s.getIsCustom())
                        && "Ghi chu".equals(s.getNote())
        ));
    }


    @Test
    void create_ShouldAttachTemplate_WhenTemplateExists() {

        UUID staffId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();

        StaffInfo staff = staff(staffId, "Nhan vien");
        ShiftConfig shift = shift(shiftId, "Ca");
        StaffScheduleTemplate template = mock(StaffScheduleTemplate.class);

        ScheduleCreateRequest req = mock(ScheduleCreateRequest.class);

        when(req.staffId()).thenReturn(staffId);
        when(req.shiftId()).thenReturn(shiftId);
        when(req.templateId()).thenReturn(templateId);
        when(req.workDate()).thenReturn(LocalDate.now());

        when(staffService.findById(staffId))
                .thenReturn(staff);

        when(shiftConfigRepo.findById(shiftId))
                .thenReturn(Optional.of(shift));

        when(templateRepo.findById(templateId))
                .thenReturn(Optional.of(template));

        when(scheduleRepo.save(any()))
                .thenAnswer(i -> {
                    StaffSchedule s = i.getArgument(0);
                    s.setScheduleId(UUID.randomUUID());
                    return s;
                });

        service.create(req);

        verify(scheduleRepo).save(argThat(s ->
                s.getTemplate() == template
        ));
    }


    @Test
    void create_ShouldAllowMissingTemplate() {

        UUID staffId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();

        StaffInfo staff = staff(staffId, "Nhan vien");
        ShiftConfig shift = shift(shiftId, "Ca");

        ScheduleCreateRequest req = mock(ScheduleCreateRequest.class);

        when(req.staffId()).thenReturn(staffId);
        when(req.shiftId()).thenReturn(shiftId);
        when(req.templateId()).thenReturn(templateId);
        when(req.workDate()).thenReturn(LocalDate.now());

        when(staffService.findById(staffId))
                .thenReturn(staff);

        when(shiftConfigRepo.findById(shiftId))
                .thenReturn(Optional.of(shift));

        when(templateRepo.findById(templateId))
                .thenReturn(Optional.empty());

        when(scheduleRepo.save(any()))
                .thenAnswer(i -> {
                    StaffSchedule s = i.getArgument(0);
                    s.setScheduleId(UUID.randomUUID());
                    return s;
                });

        service.create(req);

        verify(scheduleRepo).save(argThat(s ->
                s.getTemplate() == null
        ));
    }


    @Test
    void create_ShouldNotifyStaff_WhenProfileExists() {

        UUID staffId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();

        StaffInfo staff = staff(staffId, "Nhan vien");
        ShiftConfig shift = shift(shiftId, "Ca sang");

        ScheduleCreateRequest req = mock(ScheduleCreateRequest.class);

        when(req.staffId()).thenReturn(staffId);
        when(req.shiftId()).thenReturn(shiftId);
        when(req.workDate()).thenReturn(LocalDate.now());

        when(staffService.findById(staffId))
                .thenReturn(staff);

        when(shiftConfigRepo.findById(shiftId))
                .thenReturn(Optional.of(shift));

        when(scheduleRepo.save(any()))
                .thenAnswer(i -> {
                    StaffSchedule s = i.getArgument(0);
                    s.setScheduleId(UUID.randomUUID());
                    return s;
                });

        service.create(req);

        verify(notificationService)
                .create(argThat(n ->
                        staff.getProfile().getProfileId().equals(n.recipientId())
                                && "Phân công lịch trực mới".equals(n.title())
                                && "StaffSchedule".equals(n.relatedEntity())
                ));
    }


    @Test
    void create_ShouldIgnoreNotificationException() {

        UUID staffId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();

        StaffInfo staff = staff(staffId, "Nhan vien");
        ShiftConfig shift = shift(shiftId, "Ca");

        ScheduleCreateRequest req = mock(ScheduleCreateRequest.class);

        when(req.staffId()).thenReturn(staffId);
        when(req.shiftId()).thenReturn(shiftId);
        when(req.workDate()).thenReturn(LocalDate.now());

        when(staffService.findById(staffId))
                .thenReturn(staff);

        when(shiftConfigRepo.findById(shiftId))
                .thenReturn(Optional.of(shift));

        when(scheduleRepo.save(any()))
                .thenAnswer(i -> {
                    StaffSchedule s = i.getArgument(0);
                    s.setScheduleId(UUID.randomUUID());
                    return s;
                });

        doThrow(new RuntimeException("notify fail"))
                .when(notificationService)
                .create(any());

        assertDoesNotThrow(
                () -> service.create(req)
        );
    }


    @Test
    void create_ShouldSkipNotification_WhenProfileNull() {

        UUID staffId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();

        StaffInfo staff = StaffInfo.builder()
                .staffId(staffId)
                .staffCode("STF01")
                .profile(null)
                .systemRole(SystemRole.NURSE)
                .build();

        ShiftConfig shift = shift(shiftId, "Ca");

        ScheduleCreateRequest req = mock(ScheduleCreateRequest.class);

        when(req.staffId()).thenReturn(staffId);
        when(req.shiftId()).thenReturn(shiftId);
        when(req.workDate()).thenReturn(LocalDate.now());

        when(staffService.findById(staffId))
                .thenReturn(staff);

        when(shiftConfigRepo.findById(shiftId))
                .thenReturn(Optional.of(shift));

        when(scheduleRepo.save(any()))
                .thenAnswer(i -> {
                    StaffSchedule s = i.getArgument(0);
                    s.setScheduleId(UUID.randomUUID());
                    return s;
                });

        service.create(req);

        verifyNoInteractions(notificationService);
    }


    // =========================================================
    // UPDATE
    // =========================================================

    @Test
    void update_ShouldThrow_WhenShiftMissing() {

        UUID scheduleId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();

        StaffSchedule s = schedule(
                scheduleId,
                staff(UUID.randomUUID(), "Nhan vien"),
                LocalDate.now(),
                shift(UUID.randomUUID(), "Old")
        );

        ScheduleUpdateRequest req = mock(ScheduleUpdateRequest.class);

        when(req.shiftId()).thenReturn(shiftId);

        when(scheduleRepo.findById(scheduleId))
                .thenReturn(Optional.of(s));

        when(shiftConfigRepo.findById(shiftId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.update(scheduleId, req)
        );
    }


    @Test
    void update_ShouldUpdateAllFields() {

        UUID scheduleId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();

        StaffSchedule s = schedule(
                scheduleId,
                staff(UUID.randomUUID(), "Nhan vien"),
                LocalDate.now(),
                shift(UUID.randomUUID(), "Old")
        );

        ShiftConfig newShift = shift(shiftId, "New");

        ScheduleUpdateRequest req = mock(ScheduleUpdateRequest.class);

        when(req.shiftId()).thenReturn(shiftId);
        when(req.status()).thenReturn(ScheduleStatus.COMPLETED);
        when(req.isCustom()).thenReturn(true);
        when(req.note()).thenReturn("Updated");

        when(scheduleRepo.findById(scheduleId))
                .thenReturn(Optional.of(s));

        when(shiftConfigRepo.findById(shiftId))
                .thenReturn(Optional.of(newShift));

        when(scheduleRepo.save(s))
                .thenReturn(s);

        service.update(scheduleId, req);

        assertSame(newShift, s.getShift());
        assertEquals(ScheduleStatus.COMPLETED, s.getStatus());
        assertTrue(s.getIsCustom());
        assertEquals("Updated", s.getNote());
    }


    @Test
    void update_ShouldLeaveFieldsUnchanged_WhenRequestNulls() {

        UUID scheduleId = UUID.randomUUID();

        ShiftConfig oldShift = shift(UUID.randomUUID(), "Old");

        StaffSchedule s = schedule(
                scheduleId,
                staff(UUID.randomUUID(), "Nhan vien"),
                LocalDate.now(),
                oldShift
        );

        s.setNote("Old note");

        ScheduleUpdateRequest req = mock(ScheduleUpdateRequest.class);

        when(scheduleRepo.findById(scheduleId))
                .thenReturn(Optional.of(s));

        when(scheduleRepo.save(s))
                .thenReturn(s);

        service.update(scheduleId, req);

        assertSame(oldShift, s.getShift());
        assertEquals(ScheduleStatus.SCHEDULED, s.getStatus());
        assertFalse(s.getIsCustom());
        assertEquals("Old note", s.getNote());
    }


    @Test
    void update_ShouldNotify_WhenStaffProfileExists() {

        UUID scheduleId = UUID.randomUUID();

        StaffInfo staff = staff(UUID.randomUUID(), "Nhan vien");

        StaffSchedule s = schedule(
                scheduleId,
                staff,
                LocalDate.now(),
                shift(UUID.randomUUID(), "Ca")
        );

        ScheduleUpdateRequest req = mock(ScheduleUpdateRequest.class);

        when(scheduleRepo.findById(scheduleId))
                .thenReturn(Optional.of(s));

        when(scheduleRepo.save(s))
                .thenReturn(s);

        service.update(scheduleId, req);

        verify(notificationService)
                .create(argThat(n ->
                        "Thay đổi lịch trực".equals(n.title())
                                && "StaffSchedule".equals(n.relatedEntity())
                ));
    }


    @Test
    void update_ShouldIgnoreNotificationException() {

        UUID scheduleId = UUID.randomUUID();

        StaffSchedule s = schedule(
                scheduleId,
                staff(UUID.randomUUID(), "Nhan vien"),
                LocalDate.now(),
                shift(UUID.randomUUID(), "Ca")
        );

        ScheduleUpdateRequest req = mock(ScheduleUpdateRequest.class);

        when(scheduleRepo.findById(scheduleId))
                .thenReturn(Optional.of(s));

        when(scheduleRepo.save(s))
                .thenReturn(s);

        doThrow(new RuntimeException("fail"))
                .when(notificationService)
                .create(any());

        assertDoesNotThrow(
                () -> service.update(scheduleId, req)
        );
    }


    // =========================================================
    // DELETE
    // =========================================================

    @Test
    void delete_ShouldThrow_WhenScheduleMissing() {

        UUID id = UUID.randomUUID();

        when(scheduleRepo.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.delete(id)
        );
    }


    @Test
    void delete_ShouldDelete_WhenFound() {

        UUID id = UUID.randomUUID();

        StaffSchedule s = schedule(
                id,
                staff(UUID.randomUUID(), "Nhan vien"),
                LocalDate.now(),
                shift(UUID.randomUUID(), "Ca")
        );

        when(scheduleRepo.findById(id))
                .thenReturn(Optional.of(s));

        service.delete(id);

        verify(scheduleRepo)
                .deleteById(id);
    }


    @Test
    void delete_ShouldNotifyStaff() {

        UUID id = UUID.randomUUID();

        StaffSchedule s = schedule(
                id,
                staff(UUID.randomUUID(), "Nhan vien"),
                LocalDate.now(),
                shift(UUID.randomUUID(), "Ca")
        );

        when(scheduleRepo.findById(id))
                .thenReturn(Optional.of(s));

        service.delete(id);

        verify(notificationService)
                .create(argThat(n ->
                        "Hủy lịch trực".equals(n.title())
                                && "StaffSchedule".equals(n.relatedEntity())
                ));
    }


    @Test
    void delete_ShouldIgnoreNotificationException() {

        UUID id = UUID.randomUUID();

        StaffSchedule s = schedule(
                id,
                staff(UUID.randomUUID(), "Nhan vien"),
                LocalDate.now(),
                shift(UUID.randomUUID(), "Ca")
        );

        when(scheduleRepo.findById(id))
                .thenReturn(Optional.of(s));

        doThrow(new RuntimeException("fail"))
                .when(notificationService)
                .create(any());

        assertDoesNotThrow(
                () -> service.delete(id)
        );

        verify(scheduleRepo)
                .deleteById(id);
    }


    // =========================================================
    // GET / FIND
    // =========================================================

    @Test
    void findById_ShouldReturn_WhenFound() {

        UUID id = UUID.randomUUID();

        StaffSchedule s = schedule(
                id,
                staff(UUID.randomUUID(), "Nhan vien"),
                LocalDate.now(),
                shift(UUID.randomUUID(), "Ca")
        );

        when(scheduleRepo.findById(id))
                .thenReturn(Optional.of(s));

        assertSame(s, service.findById(id));
    }


    @Test
    void findById_ShouldThrow_WhenMissing() {

        UUID id = UUID.randomUUID();

        when(scheduleRepo.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.findById(id)
        );
    }


    @Test
    void get_ShouldReturnResponse() {

        UUID id = UUID.randomUUID();

        StaffSchedule s = schedule(
                id,
                staff(UUID.randomUUID(), "Nhan vien"),
                LocalDate.now(),
                shift(UUID.randomUUID(), "Ca")
        );

        when(scheduleRepo.findById(id))
                .thenReturn(Optional.of(s));

        assertNotNull(service.get(id));
    }


    // =========================================================
    // SEARCH
    // =========================================================

    @Test
    void search_ShouldWorkWithoutShift() {

        var pageable = PageRequest.of(0, 10);

        when(
                scheduleRepo.search(
                        null,
                        null,
                        null,
                        null,
                        pageable
                )
        ).thenReturn(
                new PageImpl<>(List.of())
        );

        assertNotNull(
                service.search(
                        null,
                        null,
                        null,
                        null,
                        pageable
                )
        );
    }


    @Test
    void search_ShouldResolveShift_WhenShiftIdProvided() {

        UUID shiftId = UUID.randomUUID();
        ShiftConfig shift = shift(shiftId, "Ca");

        var pageable = PageRequest.of(0, 10);

        when(shiftConfigRepo.findById(shiftId))
                .thenReturn(Optional.of(shift));

        when(
                scheduleRepo.search(
                        null,
                        null,
                        null,
                        shift,
                        pageable
                )
        ).thenReturn(
                new PageImpl<>(List.of())
        );

        service.search(
                null,
                null,
                null,
                shiftId,
                pageable
        );

        verify(scheduleRepo)
                .search(
                        null,
                        null,
                        null,
                        shift,
                        pageable
                );
    }


    @Test
    void search_ShouldPassNullShift_WhenShiftMissing() {

        UUID shiftId = UUID.randomUUID();

        var pageable = PageRequest.of(0, 10);

        when(shiftConfigRepo.findById(shiftId))
                .thenReturn(Optional.empty());

        when(
                scheduleRepo.search(
                        null,
                        null,
                        null,
                        null,
                        pageable
                )
        ).thenReturn(
                new PageImpl<>(List.of())
        );

        service.search(
                null,
                null,
                null,
                shiftId,
                pageable
        );
    }


    // =========================================================
    // GENERATE FROM TEMPLATES
    // =========================================================

    @Test
    void generateFromTemplates_ShouldUseAllStaffFromTemplates_WhenStaffIdsNull() {

        UUID staffId = UUID.randomUUID();

        StaffInfo staff = staff(staffId, "Nhan vien");

        StaffScheduleTemplate t1 = mock(StaffScheduleTemplate.class);
        StaffScheduleTemplate t2 = mock(StaffScheduleTemplate.class);

        when(t1.getStaff()).thenReturn(staff);
        when(t2.getStaff()).thenReturn(staff);

        when(templateRepo.findAll())
                .thenReturn(List.of(t1, t2));

        when(templateRepo.findByStaff(staff))
                .thenReturn(List.of());

        when(scheduleRepo.saveAll(anyList()))
                .thenReturn(List.of());

        var result = service.generateFromTemplates(
                LocalDate.of(2026, 8, 3),
                null,
                false
        );

        assertTrue(result.isEmpty());

        verify(templateRepo, times(1))
                .findByStaff(staff);
    }


    @Test
    void generateFromTemplates_ShouldUseSpecifiedStaffIds() {

        UUID staffId = UUID.randomUUID();

        StaffInfo staff = staff(staffId, "Nhan vien");

        when(staffService.findById(staffId))
                .thenReturn(staff);

        when(templateRepo.findByStaff(staff))
                .thenReturn(List.of());

        when(scheduleRepo.saveAll(anyList()))
                .thenReturn(List.of());

        service.generateFromTemplates(
                LocalDate.of(2026, 8, 3),
                List.of(staffId),
                false
        );

        verify(staffService)
                .findById(staffId);
    }


    @Test
    void generateFromTemplates_ShouldSkipInactiveTemplate() {

        UUID staffId = UUID.randomUUID();

        StaffInfo staff = staff(staffId, "Nhan vien");

        StaffScheduleTemplate inactive = mock(StaffScheduleTemplate.class);

        when(inactive.getIsActive())
                .thenReturn(false);

        when(staffService.findById(staffId))
                .thenReturn(staff);

        when(templateRepo.findByStaff(staff))
                .thenReturn(List.of(inactive));

        when(scheduleRepo.saveAll(anyList()))
                .thenReturn(List.of());

        service.generateFromTemplates(
                LocalDate.of(2026, 8, 3),
                List.of(staffId),
                false
        );

        verify(scheduleRepo)
                .saveAll(argThat(
                        (Iterable<StaffSchedule> iterable) ->
                                !iterable.iterator().hasNext()
                ));
    }


    @Test
    void generateFromTemplates_ShouldCreateScheduleForActiveTemplate() {

        UUID staffId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();

        StaffInfo staff = staff(staffId, "Nhan vien");
        ShiftConfig shift = shift(shiftId, "Ca");

        StaffScheduleTemplate template = mock(StaffScheduleTemplate.class);

        when(template.getIsActive()).thenReturn(true);
        when(template.getDayOfWeek()).thenReturn(DayOfWeek.WEDNESDAY);
        when(template.getShift()).thenReturn(shift);

        when(staffService.findById(staffId))
                .thenReturn(staff);

        when(templateRepo.findByStaff(staff))
                .thenReturn(List.of(template));

        when(scheduleRepo.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result =
                service.generateFromTemplates(
                        LocalDate.of(2026, 8, 3), // Monday
                        List.of(staffId),
                        false
                );

        assertEquals(1, result.size());

        verify(scheduleRepo)
                .saveAll(argThat(
                        (Iterable<StaffSchedule> iterable) -> {
                            List<StaffSchedule> list = new ArrayList<>();
                            iterable.forEach(list::add);

                            return list.size() == 1
                                    && list.get(0).getWorkDate()
                                    .equals(LocalDate.of(2026, 8, 5))
                                    && list.get(0).getShift() == shift
                                    && list.get(0).getStaff() == staff
                                    && list.get(0).getStatus()
                                    == ScheduleStatus.SCHEDULED
                                    && Boolean.FALSE.equals(
                                    list.get(0).getIsCustom()
                            );
                        }
                ));
    }


    @Test
    void generateFromTemplates_ShouldDeleteExisting_WhenOverrideTrue() {

        UUID staffId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();

        StaffInfo staff = staff(staffId, "Nhan vien");
        ShiftConfig shift = shift(shiftId, "Ca");

        StaffScheduleTemplate template = mock(StaffScheduleTemplate.class);

        when(template.getIsActive()).thenReturn(true);
        when(template.getDayOfWeek()).thenReturn(DayOfWeek.MONDAY);
        when(template.getShift()).thenReturn(shift);

        StaffSchedule existing = schedule(
                UUID.randomUUID(),
                staff,
                LocalDate.of(2026, 8, 3),
                shift
        );

        when(staffService.findById(staffId))
                .thenReturn(staff);

        when(templateRepo.findByStaff(staff))
                .thenReturn(List.of(template));

        when(
                scheduleRepo.findByStaffAndWorkDateBetween(
                        staff,
                        LocalDate.of(2026, 8, 3),
                        LocalDate.of(2026, 8, 3)
                )
        ).thenReturn(List.of(existing));

        when(scheduleRepo.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.generateFromTemplates(
                LocalDate.of(2026, 8, 3),
                List.of(staffId),
                true
        );

        verify(scheduleRepo)
                .delete(existing);
    }


    @Test
    void generateFromTemplates_ShouldNotDeleteExistingDifferentShift() {

        UUID staffId = UUID.randomUUID();

        ShiftConfig templateShift =
                shift(UUID.randomUUID(), "Ca A");

        ShiftConfig otherShift =
                shift(UUID.randomUUID(), "Ca B");

        StaffInfo staff =
                staff(staffId, "Nhan vien");

        StaffScheduleTemplate template =
                mock(StaffScheduleTemplate.class);

        when(template.getIsActive()).thenReturn(true);
        when(template.getDayOfWeek()).thenReturn(DayOfWeek.MONDAY);
        when(template.getShift()).thenReturn(templateShift);

        StaffSchedule existing =
                schedule(
                        UUID.randomUUID(),
                        staff,
                        LocalDate.of(2026, 8, 3),
                        otherShift
                );

        when(staffService.findById(staffId))
                .thenReturn(staff);

        when(templateRepo.findByStaff(staff))
                .thenReturn(List.of(template));

        when(
                scheduleRepo.findByStaffAndWorkDateBetween(
                        staff,
                        LocalDate.of(2026, 8, 3),
                        LocalDate.of(2026, 8, 3)
                )
        ).thenReturn(List.of(existing));

        when(scheduleRepo.saveAll(anyList()))
                .thenAnswer(i -> i.getArgument(0));

        service.generateFromTemplates(
                LocalDate.of(2026, 8, 3),
                List.of(staffId),
                true
        );

        verify(scheduleRepo, never())
                .delete(any(StaffSchedule.class));
    }


    // =========================================================
    // WEEK SEARCH
    // =========================================================

    @Test
    void findByWeek_ShouldDelegateToRepository() {

        LocalDate from = LocalDate.of(2026, 8, 3);
        LocalDate to = LocalDate.of(2026, 8, 9);

        when(scheduleRepo.findAllByWorkDateBetween(from, to))
                .thenReturn(List.of());

        assertTrue(
                service.findByWeek(from, to)
                        .isEmpty()
        );
    }


    @Test
    void findByStaffAndWeek_ShouldResolveStaff() {

        UUID staffId = UUID.randomUUID();

        LocalDate from = LocalDate.of(2026, 8, 3);
        LocalDate to = LocalDate.of(2026, 8, 9);

        StaffInfo staff =
                staff(staffId, "Nhan vien");

        when(staffService.findById(staffId))
                .thenReturn(staff);

        when(
                scheduleRepo.findByStaffAndWorkDateBetween(
                        staff,
                        from,
                        to
                )
        ).thenReturn(List.of());

        assertTrue(
                service.findByStaffAndWeek(
                        staffId,
                        from,
                        to
                ).isEmpty()
        );
    }


    // =========================================================
    // ASSIGN STAFF
    // =========================================================

    @Test
    void assignStaff_ShouldThrow_WhenShiftMissing() {

        UUID shiftId = UUID.randomUUID();

        ScheduleAssignRequest req = mock(ScheduleAssignRequest.class);

        when(req.week())
                .thenReturn(LocalDate.of(2026, 8, 3));

        when(req.dayKey())
                .thenReturn("mon");

        when(req.shiftId())
                .thenReturn(shiftId);

        when(shiftConfigRepo.findById(shiftId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.assignStaff(req)
        );
    }


    @Test
    void assignStaff_ShouldRejectInvalidDayKey() {

        ScheduleAssignRequest req =
                mock(ScheduleAssignRequest.class);

        when(req.week())
                .thenReturn(LocalDate.of(2026, 8, 3));

        when(req.dayKey())
                .thenReturn("xxx");

        assertThrows(
                IllegalArgumentException.class,
                () -> service.assignStaff(req)
        );

        verifyNoInteractions(shiftConfigRepo);
    }


    @Test
    void assignStaff_ShouldRemoveSchedule() {

        UUID staffId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();

        LocalDate week = LocalDate.of(2026, 8, 3);

        StaffInfo staff = staff(staffId, "Nhan vien");
        ShiftConfig shift = shift(shiftId, "Ca");

        ScheduleAssignRequest req =
                mock(ScheduleAssignRequest.class);

        when(req.week()).thenReturn(week);
        when(req.dayKey()).thenReturn("wed");
        when(req.shiftId()).thenReturn(shiftId);
        when(req.staffId()).thenReturn(staffId);
        when(req.action()).thenReturn("remove");

        when(shiftConfigRepo.findById(shiftId))
                .thenReturn(Optional.of(shift));

        when(staffService.findById(staffId))
                .thenReturn(staff);

        service.assignStaff(req);

        verify(scheduleRepo)
                .deleteByStaffAndWorkDateAndShift(
                        staff,
                        LocalDate.of(2026, 8, 5),
                        shift
                );
    }


    @Test
    void assignStaff_ShouldCreate_WhenNotExisting() {

        UUID staffId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();

        LocalDate week =
                LocalDate.of(2026, 8, 3);

        StaffInfo staff =
                staff(staffId, "Nhan vien");

        ShiftConfig shift =
                shift(shiftId, "Ca");

        ScheduleAssignRequest req =
                mock(ScheduleAssignRequest.class);

        when(req.week()).thenReturn(week);
        when(req.dayKey()).thenReturn("fri");
        when(req.shiftId()).thenReturn(shiftId);
        when(req.staffId()).thenReturn(staffId);
        when(req.action()).thenReturn("add");

        when(shiftConfigRepo.findById(shiftId))
                .thenReturn(Optional.of(shift));

        when(staffService.findById(staffId))
                .thenReturn(staff);

        when(
                scheduleRepo.findByStaffAndWorkDateAndShift(
                        staff,
                        LocalDate.of(2026, 8, 7),
                        shift
                )
        ).thenReturn(Optional.empty());

        service.assignStaff(req);

        verify(scheduleRepo)
                .save(argThat(s ->
                        s.getStaff() == staff
                                && s.getShift() == shift
                                && s.getWorkDate()
                                .equals(LocalDate.of(2026, 8, 7))
                                && s.getStatus()
                                == ScheduleStatus.SCHEDULED
                                && Boolean.TRUE.equals(
                                s.getIsCustom()
                        )
                ));
    }


    @Test
    void assignStaff_ShouldSkipCreate_WhenAlreadyExists() {

        UUID staffId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();

        LocalDate week =
                LocalDate.of(2026, 8, 3);

        StaffInfo staff =
                staff(staffId, "Nhan vien");

        ShiftConfig shift =
                shift(shiftId, "Ca");

        ScheduleAssignRequest req =
                mock(ScheduleAssignRequest.class);

        when(req.week()).thenReturn(week);
        when(req.dayKey()).thenReturn("mon");
        when(req.shiftId()).thenReturn(shiftId);
        when(req.staffId()).thenReturn(staffId);
        when(req.action()).thenReturn("add");

        when(shiftConfigRepo.findById(shiftId))
                .thenReturn(Optional.of(shift));

        when(staffService.findById(staffId))
                .thenReturn(staff);

        when(
                scheduleRepo.findByStaffAndWorkDateAndShift(
                        staff,
                        week,
                        shift
                )
        ).thenReturn(
                Optional.of(
                        schedule(
                                UUID.randomUUID(),
                                staff,
                                week,
                                shift
                        )
                )
        );

        service.assignStaff(req);

        verify(scheduleRepo, never())
                .save(any());
    }


    // =========================================================
    // COPY WEEK
    // =========================================================

    @Test
    void copyWeek_ShouldReturnEmpty_WhenOldWeekHasNoSchedules() {

        LocalDate from =
                LocalDate.of(2026, 8, 3);

        LocalDate to =
                LocalDate.of(2026, 8, 10);

        when(
                scheduleRepo.findAllByWorkDateBetween(
                        from,
                        from.plusDays(6)
                )
        ).thenReturn(List.of());

        when(scheduleRepo.saveAll(anyList()))
                .thenReturn(List.of());

        assertTrue(
                service.copyWeek(from, to)
                        .isEmpty()
        );
    }


    @Test
    void copyWeek_ShouldCopyScheduleToSameDayOfWeek() {

        LocalDate from =
                LocalDate.of(2026, 8, 3);

        LocalDate to =
                LocalDate.of(2026, 8, 10);

        StaffInfo staff =
                staff(UUID.randomUUID(), "Nhan vien");

        ShiftConfig shift =
                shift(UUID.randomUUID(), "Ca");

        StaffSchedule old =
                schedule(
                        UUID.randomUUID(),
                        staff,
                        LocalDate.of(2026, 8, 5), // Wednesday
                        shift
                );

        old.setStatus(ScheduleStatus.COMPLETED);
        old.setNote("Old note");

        when(
                scheduleRepo.findAllByWorkDateBetween(
                        from,
                        from.plusDays(6)
                )
        ).thenReturn(List.of(old));

        when(
                scheduleRepo.findByStaffAndWorkDateAndShift(
                        staff,
                        LocalDate.of(2026, 8, 12),
                        shift
                )
        ).thenReturn(Optional.empty());

        when(scheduleRepo.saveAll(anyList()))
                .thenAnswer(i -> i.getArgument(0));

        var result =
                service.copyWeek(
                        from,
                        to
                );

        assertEquals(1, result.size());

        StaffSchedule copied = result.get(0);

        assertEquals(
                LocalDate.of(2026, 8, 12),
                copied.getWorkDate()
        );

        assertSame(staff, copied.getStaff());
        assertSame(shift, copied.getShift());

        assertEquals(
                ScheduleStatus.COMPLETED,
                copied.getStatus()
        );

        assertTrue(copied.getIsCustom());

        assertEquals(
                "Old note",
                copied.getNote()
        );
    }


    @Test
    void copyWeek_ShouldSkip_WhenScheduleAlreadyExists() {

        LocalDate from =
                LocalDate.of(2026, 8, 3);

        LocalDate to =
                LocalDate.of(2026, 8, 10);

        StaffInfo staff =
                staff(UUID.randomUUID(), "Nhan vien");

        ShiftConfig shift =
                shift(UUID.randomUUID(), "Ca");

        StaffSchedule old =
                schedule(
                        UUID.randomUUID(),
                        staff,
                        LocalDate.of(2026, 8, 3),
                        shift
                );

        when(
                scheduleRepo.findAllByWorkDateBetween(
                        from,
                        from.plusDays(6)
                )
        ).thenReturn(List.of(old));

        when(
                scheduleRepo.findByStaffAndWorkDateAndShift(
                        staff,
                        to,
                        shift
                )
        ).thenReturn(
                Optional.of(
                        schedule(
                                UUID.randomUUID(),
                                staff,
                                to,
                                shift
                        )
                )
        );

        when(scheduleRepo.saveAll(anyList()))
                .thenReturn(List.of());

        var result =
                service.copyWeek(
                        from,
                        to
                );

        assertTrue(result.isEmpty());

        verify(scheduleRepo)
                .saveAll(argThat(
                        (Iterable<StaffSchedule> iterable) ->
                                !iterable.iterator().hasNext()
                ));
    }
}