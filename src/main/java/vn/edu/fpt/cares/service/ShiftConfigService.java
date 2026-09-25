package vn.edu.fpt.cares.service;

import lombok.RequiredArgsConstructor;
import vn.edu.fpt.cares.dto.shift.ShiftConfigResponse;
import vn.edu.fpt.cares.model.ShiftConfig;
import vn.edu.fpt.cares.model.ShiftVersion;
import vn.edu.fpt.cares.repository.ShiftConfigRepository;
import vn.edu.fpt.cares.repository.ShiftVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class ShiftConfigService {

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final String MORNING = "Ca Sáng";
    public static final String AFTERNOON = "Ca Chiều";
    public static final String EVENING = "Ca Tối";
    public static final Set<String> FIXED_SHIFT_NAMES = Set.of(MORNING, AFTERNOON, EVENING);
    private static final Map<String, Integer> SHIFT_ORDER = Map.of(MORNING, 1, AFTERNOON, 2, EVENING, 3);
    private static final List<DefaultShift> DEFAULT_SHIFTS = List.of(
            new DefaultShift(MORNING, "00:00", "08:00"),
            new DefaultShift(AFTERNOON, "08:00", "16:00"),
            new DefaultShift(EVENING, "16:00", "23:59:59")
    );

    private final ShiftConfigRepository shiftConfigRepository;
    private final ShiftVersionRepository shiftVersionRepository;

    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void initDefaultShifts() {
        for (DefaultShift definition : DEFAULT_SHIFTS) {
            ShiftConfig shift = shiftConfigRepository.findFirstByNameIgnoreCase(definition.name())
                    .orElseGet(() -> shiftConfigRepository.save(ShiftConfig.builder()
                            .name(definition.name())
                            .startTime(definition.startTime())
                            .endTime(definition.endTime())
                            .isActive(true)
                            .build()));
            if (!definition.startTime().equals(shift.getStartTime())
                    || !definition.endTime().equals(shift.getEndTime())) {
                shift.setStartTime(definition.startTime());
                shift.setEndTime(definition.endTime());
            }
            if (!Boolean.TRUE.equals(shift.getIsActive())) {
                shift.setIsActive(true);
            }
            shiftConfigRepository.save(shift);
            synchronizeDefaultVersion(shift, definition);
        }
    }

    @Transactional(readOnly = true)
    public List<ShiftConfigResponse> getAllActiveShifts() {
        return fixedShifts().stream().map(this::currentResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ShiftConfigResponse> getAllShifts() {
        return fixedShifts().stream().map(this::currentResponse).toList();
    }

    public static boolean isFixedShift(ShiftConfig shift) {
        return shift != null && FIXED_SHIFT_NAMES.contains(shift.getName());
    }

    public static int fixedOrder(ShiftConfig shift) {
        return shift == null ? Integer.MAX_VALUE : SHIFT_ORDER.getOrDefault(shift.getName(), Integer.MAX_VALUE);
    }

    private List<ShiftConfig> fixedShifts() {
        return shiftConfigRepository.findAll().stream()
                .filter(ShiftConfigService::isFixedShift)
                .sorted(Comparator.comparingInt(ShiftConfigService::fixedOrder))
                .toList();
    }

    private void synchronizeDefaultVersion(ShiftConfig shift, DefaultShift definition) {
        List<ShiftVersion> versions = shiftVersionRepository
                .findAllByShift_ShiftIdOrderByEffectiveFromDesc(shift.getShiftId());
        LocalTime defaultStart = LocalTime.parse(definition.startTime());
        LocalTime defaultEnd = LocalTime.parse(definition.endTime());

        if (versions.isEmpty()) {
            shiftVersionRepository.save(ShiftVersion.builder()
                    .shift(shift)
                    .startTime(defaultStart)
                    .endTime(defaultEnd)
                    .effectiveFrom(LocalDate.of(1970, 1, 1))
                    .changeReason("Khởi tạo khung giờ cố định " + shift.getName().toLowerCase())
                    .build());
            return;
        }

        // Chỉ nâng cấp dữ liệu nền cũ khi chưa từng có phiên bản do người dùng tạo.
        // Phiên bản cũ được giữ lại để lịch hẹn/lịch trực đã phát sinh vẫn có snapshot.
        if (versions.size() == 1) {
            ShiftVersion baseline = versions.get(0);
            boolean alreadyUsesDefault = defaultStart.equals(baseline.getStartTime())
                    && defaultEnd.equals(baseline.getEndTime());
            LocalDate today = LocalDate.now(CLINIC_ZONE);
            if (!alreadyUsesDefault && baseline.getEffectiveFrom().isBefore(today)) {
                baseline.setEffectiveTo(today.minusDays(1));
                shiftVersionRepository.save(baseline);
                shiftVersionRepository.save(ShiftVersion.builder()
                        .shift(shift)
                        .startTime(defaultStart)
                        .endTime(defaultEnd)
                        .effectiveFrom(today)
                        .changeReason("Chuẩn hóa khung giờ kiểm thử 24 giờ")
                        .build());
            }
        }
    }

    private ShiftConfigResponse currentResponse(ShiftConfig shift) {
        var version = shiftVersionRepository.findEffective(shift.getShiftId(),
                LocalDate.now(CLINIC_ZONE)).stream().findFirst().orElse(null);
        return new ShiftConfigResponse(shift.getShiftId(), shift.getName(),
                version == null ? shift.getStartTime() : version.getStartTime().toString(),
                version == null ? shift.getEndTime() : version.getEndTime().toString(), true);
    }

    private record DefaultShift(String name, String startTime, String endTime) {}
}
