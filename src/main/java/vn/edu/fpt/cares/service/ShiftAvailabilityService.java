package vn.edu.fpt.cares.service;

import lombok.RequiredArgsConstructor;
import vn.edu.fpt.cares.dto.shift.AvailableShiftResponse;
import vn.edu.fpt.cares.enums.ShiftUnavailableReason;
import vn.edu.fpt.cares.exception.ResourceNotFoundException;
import vn.edu.fpt.cares.model.*;
import vn.edu.fpt.cares.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShiftAvailabilityService {
    private final ShiftConfigRepository shiftRepository;
    private final MedicalServiceRepository serviceRepository;
    private final ShiftScheduleResolver resolver;
    private final ServiceAvailabilityService serviceAvailabilityService;

    public List<AvailableShiftResponse> available(LocalDate date, Set<UUID> serviceIds) {
        List<MedicalService> services = new ArrayList<>();
        if (serviceIds != null) {
            for (UUID id : serviceIds) services.add(serviceRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Dịch vụ không tồn tại: " + id)));
        }
        List<AvailableShiftResponse> result = new ArrayList<>();
        List<ShiftConfig> fixedShifts = shiftRepository.findAllByIsActiveTrueOrderByStartTimeAsc().stream()
                .filter(ShiftConfigService::isFixedShift)
                .sorted(Comparator.comparingInt(ShiftConfigService::fixedOrder))
                .toList();
        for (ShiftConfig shift : fixedShifts) {
            ShiftScheduleResolver.ResolvedShift resolved = resolver.resolve(shift, date);
            Map<UUID, ShiftUnavailableReason> reasons = new LinkedHashMap<>();
            if (resolved.available()) {
                for (MedicalService service : services) {
                    ServiceAvailabilityService.Evaluation evaluation =
                            serviceAvailabilityService.evaluate(service, date, shift, true);
                    if (!evaluation.available()) reasons.put(service.getServiceId(), evaluation.reason());
                }
            }
            ShiftUnavailableReason overall = resolved.unavailableReason();
            if (overall == null && !reasons.isEmpty()) overall = reasons.values().iterator().next();
            result.add(new AvailableShiftResponse(shift.getShiftId(),
                    resolved.version() == null ? null : resolved.version().getShiftVersionId(), shift.getName(),
                    resolved.startTime(), resolved.endTime(), overall == null, resolved.source(), overall, reasons));
        }
        return result;
    }
}
