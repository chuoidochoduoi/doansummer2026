package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.Appointment;
import org.example.doansummer2026.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.Collection;
import java.util.List;
import java.time.LocalDateTime;
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

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Appointment a " +
           "WHERE a.customer.profileId = :customerId AND a.status IN :statuses " +
           "AND a.scheduledAt >= :from AND a.scheduledAt < :to")
    boolean existsCustomerConflict(@Param("customerId") UUID customerId,
                                   @Param("statuses") Collection<AppointmentStatus> statuses,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Appointment a " +
           "WHERE a.customer.profileId = :customerId AND a.appointmentId <> :appointmentId " +
           "AND a.status IN :statuses AND a.scheduledAt >= :from AND a.scheduledAt < :to")
    boolean existsOtherCustomerConflict(@Param("customerId") UUID customerId,
                                        @Param("appointmentId") UUID appointmentId,
                                        @Param("statuses") Collection<AppointmentStatus> statuses,
                                        @Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Appointment a " +
           "WHERE a.isGuest = true AND a.status IN :statuses " +
           "AND ((:phone IS NOT NULL AND a.guestPhone = :phone) OR (:email IS NOT NULL AND a.guestEmail = :email)) " +
           "AND a.scheduledAt >= :from AND a.scheduledAt < :to")
    boolean existsGuestConflict(@Param("phone") String phone,
                                @Param("email") String email,
                                @Param("statuses") Collection<AppointmentStatus> statuses,
                                @Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to);
    
    @Query("SELECT a FROM Appointment a WHERE a.isGuest = true AND (a.guestPhone IN :phones OR a.guestEmail IN :emails)")
    List<Appointment> findGuestAppointmentsByPhonesOrEmails(@Param("phones") Collection<String> phones, @Param("emails") Collection<String> emails);

}



