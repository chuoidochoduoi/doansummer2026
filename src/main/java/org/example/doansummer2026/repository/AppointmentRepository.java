package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.Appointment;
import org.example.doansummer2026.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.Collection;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID>, AppointmentRepositoryCustom {

    @EntityGraph("Appointment.withDetails")
    Optional<Appointment> findById(UUID id);

    boolean existsByCustomer_ProfileIdAndStatusIn(UUID customerId, Collection<AppointmentStatus> statuses);
}



