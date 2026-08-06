package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.ShiftConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShiftConfigRepository extends JpaRepository<ShiftConfig, UUID> {
    List<ShiftConfig> findAllByIsActiveTrueOrderByStartTimeAsc();
    List<ShiftConfig> findAllByOrderByStartTimeAsc();
}
