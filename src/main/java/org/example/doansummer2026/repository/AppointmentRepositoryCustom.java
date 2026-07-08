package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AppointmentRepositoryCustom {
    Page<Appointment> search(UUID customerId, String status,
                            LocalDateTime from, LocalDateTime to, Pageable pageable);
}