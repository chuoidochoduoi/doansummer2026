package org.example.doansummer2026.repository;
import org.example.doansummer2026.model.AttendanceAdjustment;
import org.example.doansummer2026.enums.AttendanceAdjustmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface AttendanceAdjustmentRepository extends JpaRepository<AttendanceAdjustment,UUID> { List<AttendanceAdjustment> findAllByStatusOrderByCreatedAtAsc(AttendanceAdjustmentStatus status); }
