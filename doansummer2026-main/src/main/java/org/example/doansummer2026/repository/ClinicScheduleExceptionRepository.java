package org.example.doansummer2026.repository;

import jakarta.persistence.LockModeType;
import org.example.doansummer2026.model.ClinicScheduleException;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.*;

public interface ClinicScheduleExceptionRepository extends JpaRepository<ClinicScheduleException, UUID> {
    List<ClinicScheduleException> findAllByWorkDate(LocalDate workDate);
    List<ClinicScheduleException> findAllByWorkDateBetweenOrderByWorkDateAsc(LocalDate from, LocalDate to);
    Optional<ClinicScheduleException> findByWorkDateAndShift_ShiftId(LocalDate workDate, UUID shiftId);
    Optional<ClinicScheduleException> findByWorkDateAndShiftIsNull(LocalDate workDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from ClinicScheduleException e where e.workDate = :date")
    List<ClinicScheduleException> findAllByWorkDateForUpdate(@Param("date") LocalDate date);
}
