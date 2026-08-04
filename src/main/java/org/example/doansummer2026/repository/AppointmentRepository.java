package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.Appointment;
import org.example.doansummer2026.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.Collection;
import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID>, AppointmentRepositoryCustom {

    @EntityGraph("Appointment.withDetails")
    Optional<Appointment> findById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Appointment a WHERE a.appointmentId = :id")
    Optional<Appointment> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByCustomer_ProfileIdAndStatusIn(UUID customerId, Collection<AppointmentStatus> statuses);
    List<Appointment> findAllByIsGuestTrueAndGuestPhoneIn(Collection<String> phones);
}



