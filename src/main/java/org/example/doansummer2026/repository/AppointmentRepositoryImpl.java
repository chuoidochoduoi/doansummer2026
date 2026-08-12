package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class AppointmentRepositoryImpl implements AppointmentRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<Appointment> search(UUID customerId, String status,
                                   LocalDateTime from, LocalDateTime to, Pageable pageable) {
        // Query chĂ­nh Ä‘á»ƒ láº¥y dá»¯ liá»‡u
        StringBuilder jpql = new StringBuilder(
            "SELECT DISTINCT a FROM Appointment a LEFT JOIN FETCH a.customer LEFT JOIN FETCH a.services WHERE a.deleted = false"
        );
        StringBuilder countJpql = new StringBuilder(
            "SELECT COUNT(DISTINCT a) FROM Appointment a WHERE a.deleted = false"
        );

        if (customerId != null) {
            jpql.append(" AND a.customer.profileId = :customerId");
            countJpql.append(" AND a.customer.profileId = :customerId");
        }
        if (status != null && !status.isEmpty()) {
            jpql.append(" AND a.status = :status");
            countJpql.append(" AND a.status = :status");
        }
        if (from != null) {
            jpql.append(" AND a.scheduledAt >= :from");
            countJpql.append(" AND a.scheduledAt >= :from");
        }
        if (to != null) {
            jpql.append(" AND a.scheduledAt <= :to");
            countJpql.append(" AND a.scheduledAt <= :to");
        }
        
        jpql.append(" ORDER BY a.createdAt DESC");

        TypedQuery<Appointment> query = em.createQuery(jpql.toString(), Appointment.class);
        TypedQuery<Long> countQuery = em.createQuery(countJpql.toString(), Long.class);

        if (customerId != null) {
            query.setParameter("customerId", customerId);
            countQuery.setParameter("customerId", customerId);
        }
        if (status != null && !status.isEmpty()) {
            org.example.doansummer2026.enums.AppointmentStatus enumStatus = org.example.doansummer2026.enums.AppointmentStatus.valueOf(status);
            query.setParameter("status", enumStatus);
            countQuery.setParameter("status", enumStatus);
        }
        if (from != null) {
            query.setParameter("from", from);
            countQuery.setParameter("from", from);
        }
        if (to != null) {
            query.setParameter("to", to);
            countQuery.setParameter("to", to);
        }

        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<Appointment> content = query.getResultList();
        Long total = countQuery.getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<Appointment> searchForCustomer(UUID customerId, String code, String specialty, String status, Pageable pageable) {
        StringBuilder jpql = new StringBuilder(
            "SELECT DISTINCT a FROM Appointment a LEFT JOIN FETCH a.customer LEFT JOIN FETCH a.services s LEFT JOIN FETCH s.department d WHERE a.deleted = false AND a.customer.profileId = :customerId"
        );
        StringBuilder countJpql = new StringBuilder(
            "SELECT COUNT(DISTINCT a) FROM Appointment a WHERE a.deleted = false AND a.customer.profileId = :customerId"
        );

        if (code != null && !code.isEmpty()) {
            jpql.append(" AND CAST(a.appointmentId AS string) LIKE :code");
            countJpql.append(" AND CAST(a.appointmentId AS string) LIKE :code");
        }
        if (specialty != null && !specialty.isEmpty()) {
            jpql.append(" AND EXISTS (SELECT 1 FROM a.services s2 JOIN s2.department d2 WHERE d2.name = :specialty)");
            countJpql.append(" AND EXISTS (SELECT 1 FROM a.services s2 JOIN s2.department d2 WHERE d2.name = :specialty)");
        }
        if (status != null && !status.isEmpty()) {
            if ("upcoming".equalsIgnoreCase(status)) {
                jpql.append(" AND a.status IN (:statusList)");
                countJpql.append(" AND a.status IN (:statusList)");
            } else if ("completed".equalsIgnoreCase(status)) {
                jpql.append(" AND a.visit.status = :visitStatus");
                countJpql.append(" AND a.visit.status = :visitStatus");
            } else if ("cancelled".equalsIgnoreCase(status)) {
                jpql.append(" AND a.status = :appStatus");
                countJpql.append(" AND a.status = :appStatus");
            } else if ("checked_in".equalsIgnoreCase(status)) {
                jpql.append(" AND a.status = :appStatus");
                countJpql.append(" AND a.status = :appStatus");
            }
        }

        jpql.append(" ORDER BY a.scheduledAt DESC");

        TypedQuery<Appointment> query = em.createQuery(jpql.toString(), Appointment.class);
        TypedQuery<Long> countQuery = em.createQuery(countJpql.toString(), Long.class);

        query.setParameter("customerId", customerId);
        countQuery.setParameter("customerId", customerId);

        if (code != null && !code.isEmpty()) {
            // Frontend sends APPT-XXXX, but UUID is lowercase with hyphens, we just search wildcard
            String searchCode = "%" + code.replace("APPT-", "").toLowerCase() + "%";
            query.setParameter("code", searchCode);
            countQuery.setParameter("code", searchCode);
        }
        if (specialty != null && !specialty.isEmpty()) {
            query.setParameter("specialty", specialty);
            countQuery.setParameter("specialty", specialty);
        }
        if (status != null && !status.isEmpty()) {
            if ("upcoming".equalsIgnoreCase(status)) {
                java.util.List<org.example.doansummer2026.enums.AppointmentStatus> statusList = java.util.List.of(
                    org.example.doansummer2026.enums.AppointmentStatus.PENDING);
                query.setParameter("statusList", statusList);
                countQuery.setParameter("statusList", statusList);
            } else if ("completed".equalsIgnoreCase(status)) {
                query.setParameter("visitStatus", org.example.doansummer2026.enums.VisitStatus.COMPLETED);
                countQuery.setParameter("visitStatus", org.example.doansummer2026.enums.VisitStatus.COMPLETED);
            } else if ("cancelled".equalsIgnoreCase(status)) {
                query.setParameter("appStatus", org.example.doansummer2026.enums.AppointmentStatus.CANCELLED);
                countQuery.setParameter("appStatus", org.example.doansummer2026.enums.AppointmentStatus.CANCELLED);
            } else if ("checked_in".equalsIgnoreCase(status)) {
                query.setParameter("appStatus", org.example.doansummer2026.enums.AppointmentStatus.CHECKED_IN);
                countQuery.setParameter("appStatus", org.example.doansummer2026.enums.AppointmentStatus.CHECKED_IN);
            }
        }

        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<Appointment> content = query.getResultList();
        Long total = countQuery.getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public List<Appointment> findByCustomerId(UUID customerId) {
        String jpql = "SELECT a FROM Appointment a LEFT JOIN FETCH a.customer LEFT JOIN FETCH a.services WHERE a.customer.profileId = :customerId AND a.deleted = false";
        return em.createQuery(jpql, Appointment.class)
                .setParameter("customerId", customerId)
                .getResultList();
    }

    @Override
    public List<Appointment> findGuestAppointmentsByPhone(String phone) {
        String jpql = "SELECT a FROM Appointment a LEFT JOIN FETCH a.services WHERE a.isGuest = true AND a.guestPhone = :phone AND a.deleted = false ORDER BY a.scheduledAt DESC";
        return em.createQuery(jpql, Appointment.class)
                .setParameter("phone", phone)
                .getResultList();
    }
}


