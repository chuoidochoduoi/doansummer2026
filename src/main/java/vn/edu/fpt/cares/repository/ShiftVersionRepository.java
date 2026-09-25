package vn.edu.fpt.cares.repository;

import jakarta.persistence.LockModeType;
import vn.edu.fpt.cares.model.ShiftVersion;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.*;

public interface ShiftVersionRepository extends JpaRepository<ShiftVersion, UUID> {
    @Query("select v from ShiftVersion v where v.shift.shiftId = :shiftId " +
            "and v.effectiveFrom <= :date and (v.effectiveTo is null or v.effectiveTo >= :date) " +
            "order by v.effectiveFrom desc")
    List<ShiftVersion> findEffective(@Param("shiftId") UUID shiftId, @Param("date") LocalDate date);

    List<ShiftVersion> findAllByShift_ShiftIdOrderByEffectiveFromDesc(UUID shiftId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from ShiftVersion v where v.shift.shiftId = :shiftId order by v.effectiveFrom")
    List<ShiftVersion> findAllByShiftForUpdate(@Param("shiftId") UUID shiftId);

    @Query("select v from ShiftVersion v where v.effectiveFrom <= :date " +
            "and (v.effectiveTo is null or v.effectiveTo >= :date)")
    List<ShiftVersion> findAllEffectiveOn(@Param("date") LocalDate date);

    boolean existsByShift_ShiftId(UUID shiftId);
}
