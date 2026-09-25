package vn.edu.fpt.cares.repository;

import jakarta.persistence.LockModeType;
import vn.edu.fpt.cares.model.ShiftConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShiftConfigRepository extends JpaRepository<ShiftConfig, UUID> {
    List<ShiftConfig> findAllByIsActiveTrueOrderByStartTimeAsc();
    Optional<ShiftConfig> findFirstByNameIgnoreCase(String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT shift FROM ShiftConfig shift WHERE shift.shiftId = :shiftId")
    Optional<ShiftConfig> findByIdForScheduleUpdate(@Param("shiftId") UUID shiftId);
}
