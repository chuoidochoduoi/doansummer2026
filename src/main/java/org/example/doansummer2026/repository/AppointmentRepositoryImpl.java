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
        // Query chính để lấy dữ liệu
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

        TypedQuery<Appointment> query = em.createQuery(jpql.toString(), Appointment.class);
        TypedQuery<Long> countQuery = em.createQuery(countJpql.toString(), Long.class);

        if (customerId != null) {
            query.setParameter("customerId", customerId);
            countQuery.setParameter("customerId", customerId);
        }
        if (status != null && !status.isEmpty()) {
            query.setParameter("status", status);
            countQuery.setParameter("status", status);
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



