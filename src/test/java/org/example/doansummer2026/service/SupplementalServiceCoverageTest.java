package org.example.doansummer2026.service;

import org.example.doansummer2026.enums.ServiceStatus;
import org.example.doansummer2026.enums.ShiftTimeSource;
import org.example.doansummer2026.enums.ShiftUnavailableReason;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.Insurance;
import org.example.doansummer2026.model.MedicalService;
import org.example.doansummer2026.model.ShiftConfig;
import org.example.doansummer2026.model.ShiftVersion;
import org.example.doansummer2026.repository.InsuranceRepository;
import org.example.doansummer2026.repository.MedicalServiceRepository;
import org.example.doansummer2026.repository.ShiftConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplementalServiceCoverageTest {

    @Mock ShiftConfigRepository shifts;
    @Mock MedicalServiceRepository medicalServices;
    @Mock ShiftScheduleResolver resolver;
    @Mock ServiceAvailabilityService availability;
    @Mock InsuranceRepository insurances;
    @Mock RestTemplate restTemplate;

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "<null>|Xin chào!",
            "'   '|Xin chào!",
            "giờ làm việc|7:30",
            "thời gian mở cửa|7:30",
            "địa chỉ phòng khám|123 Đường ABC",
            "phòng khám ở đâu|123 Đường ABC",
            "bảng giá|150.000",
            "chi phí khám|150.000",
            "gặp lễ tân|[HANDOVER]",
            "gặp nhân viên|[HANDOVER]",
            "gặp người thật|[HANDOVER]",
            "nội dung khác|chưa hiểu"
    })
    void ruleBasedBotCoversEverySupportedIntent(String input, String expected) {
        String message = "<null>".equals(input) ? null : input;
        assertTrue(new RuleBasedBotService().getBotResponse(message).contains(expected));
    }

    @Test
    void shiftAvailabilityHandlesMissingServicesAndUnavailableShift() {
        UUID missing = UUID.randomUUID();
        ShiftAvailabilityService service = new ShiftAvailabilityService(shifts, medicalServices, resolver, availability);
        when(medicalServices.findById(missing)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.available(LocalDate.now(), Set.of(missing)));

        ShiftConfig morning = shift("Ca Sáng");
        ShiftConfig custom = shift("Ca tùy chỉnh");
        when(shifts.findAllByIsActiveTrueOrderByStartTimeAsc()).thenReturn(List.of(custom, morning));
        when(resolver.resolve(morning, LocalDate.now())).thenReturn(new ShiftScheduleResolver.ResolvedShift(
                morning, null, null, null, ShiftTimeSource.NORMAL, ShiftUnavailableReason.SHIFT_OFF));

        var rows = service.available(LocalDate.now(), null);
        assertEquals(1, rows.size());
        assertFalse(rows.get(0).available());
        assertEquals(ShiftUnavailableReason.SHIFT_OFF, rows.get(0).unavailableReasonCode());
    }

    @Test
    void shiftAvailabilitySortsFixedShiftsAndReportsServiceReasons() {
        LocalDate date = LocalDate.now();
        ShiftConfig morning = shift("Ca Sáng");
        ShiftConfig evening = shift("Ca Tối");
        MedicalService first = MedicalService.builder().serviceId(UUID.randomUUID())
                .name("Khám").status(ServiceStatus.ACTIVE).build();
        MedicalService second = MedicalService.builder().serviceId(UUID.randomUUID())
                .name("Xét nghiệm").status(ServiceStatus.ACTIVE).build();
        ShiftVersion version = ShiftVersion.builder().shiftVersionId(UUID.randomUUID()).shift(morning)
                .startTime(LocalTime.of(7, 0)).endTime(LocalTime.NOON).effectiveFrom(date).changeReason("test").build();
        ShiftAvailabilityService service = new ShiftAvailabilityService(shifts, medicalServices, resolver, availability);
        when(medicalServices.findById(first.getServiceId())).thenReturn(Optional.of(first));
        when(medicalServices.findById(second.getServiceId())).thenReturn(Optional.of(second));
        when(shifts.findAllByIsActiveTrueOrderByStartTimeAsc()).thenReturn(List.of(evening, morning));
        when(resolver.resolve(morning, date)).thenReturn(new ShiftScheduleResolver.ResolvedShift(
                morning, version, LocalTime.of(7, 0), LocalTime.NOON, ShiftTimeSource.NORMAL, null));
        when(resolver.resolve(evening, date)).thenReturn(new ShiftScheduleResolver.ResolvedShift(
                evening, null, LocalTime.of(16, 0), LocalTime.of(23, 59), ShiftTimeSource.NORMAL, null));
        when(availability.evaluate(first, date, morning, true))
                .thenReturn(new ServiceAvailabilityService.Evaluation(true, null, List.of()));
        when(availability.evaluate(second, date, morning, true))
                .thenReturn(new ServiceAvailabilityService.Evaluation(false, ShiftUnavailableReason.NO_QUALIFIED_STAFF, List.of()));
        when(availability.evaluate(first, date, evening, true))
                .thenReturn(new ServiceAvailabilityService.Evaluation(true, null, List.of()));
        when(availability.evaluate(second, date, evening, true))
                .thenReturn(new ServiceAvailabilityService.Evaluation(true, null, List.of()));

        Set<UUID> ids = new LinkedHashSet<>(List.of(first.getServiceId(), second.getServiceId()));
        var rows = service.available(date, ids);

        assertEquals(List.of("Ca Sáng", "Ca Tối"), rows.stream().map(row -> row.name()).toList());
        assertFalse(rows.get(0).available());
        assertEquals(version.getShiftVersionId(), rows.get(0).shiftVersionId());
        assertEquals(Map.of(second.getServiceId(), ShiftUnavailableReason.NO_QUALIFIED_STAFF),
                rows.get(0).serviceUnavailableReasons());
        assertTrue(rows.get(1).available());
    }

    @Test
    void bhxhMockAndRemoteBranchesReturnSafeResults() {
        Insurance insurance = Insurance.builder().insuranceId(UUID.randomUUID()).code("BHYT")
                .name("Bảo hiểm Y tế").build();
        BhxhIntegrationService service = new BhxhIntegrationService(insurances, restTemplate, "http://mock/verify");

        when(insurances.findByCode("BHYT")).thenReturn(Optional.empty(), Optional.of(insurance));
        var mockWithoutConfig = service.checkBhytCard("BHYT123456");
        var mockWithConfig = service.checkBhytCard("BHYT123456");
        assertTrue(mockWithoutConfig.isValid());
        assertNull(mockWithoutConfig.insuranceId());
        assertEquals(insurance.getInsuranceId(), mockWithConfig.insuranceId());

        when(restTemplate.getForObject(anyString(), org.mockito.ArgumentMatchers.eq(Map.class)))
                .thenReturn(null, Map.of("isValid", false, "message", "Hết hạn"),
                        Map.of("isValid", false, "message", 12),
                        Map.of("isValid", true),
                        Map.of("isValid", true, "fullName", "Nguyễn An", "dateOfBirth", "2000-01-01"))
                .thenThrow(new RuntimeException("offline"));
        assertFalse(service.checkBhytCard("A").isValid());
        assertEquals("Hết hạn", service.checkBhytCard("B").message());
        assertEquals("Không nhận được kết quả xác minh BHYT", service.checkBhytCard("C").message());
        when(insurances.findByCode("BHYT")).thenReturn(Optional.empty(), Optional.of(insurance));
        assertEquals("Hệ thống chưa cấu hình loại bảo hiểm BHYT", service.checkBhytCard("D").message());
        var valid = service.checkBhytCard("E");
        assertTrue(valid.isValid());
        assertEquals("Nguyễn An", valid.fullName());
        assertEquals("2000-01-01", valid.dateOfBirth());
        assertEquals("Không thể kết nối hệ thống xác minh BHYT. Vui lòng thử lại sau.",
                service.checkBhytCard("F").message());
    }

    @Test
    void bhxhValidResponseTreatsNonStringIdentityFieldsAsAbsent() {
        Insurance insurance = Insurance.builder().insuranceId(UUID.randomUUID()).code("BHYT")
                .name("Bảo hiểm Y tế").build();
        BhxhIntegrationService service = new BhxhIntegrationService(insurances, restTemplate, "http://mock/verify");
        when(restTemplate.getForObject(anyString(), org.mockito.ArgumentMatchers.eq(Map.class)))
                .thenReturn(Map.of("isValid", true, "fullName", 123, "dateOfBirth", true));
        when(insurances.findByCode("BHYT")).thenReturn(Optional.of(insurance));

        var result = service.checkBhytCard("REMOTE");

        assertTrue(result.isValid());
        assertNull(result.fullName());
        assertNull(result.dateOfBirth());
    }

    private ShiftConfig shift(String name) {
        return ShiftConfig.builder().shiftId(UUID.randomUUID()).name(name)
                .startTime("00:00").endTime("08:00").isActive(true).build();
    }
}
