package vn.edu.fpt.cares.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import vn.edu.fpt.cares.model.Appointment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AppointmentRepositoryImplTest {
    private final EntityManager em = mock(EntityManager.class);
    private final TypedQuery<Appointment> dataQuery = mock(TypedQuery.class);
    private final TypedQuery<Long> countQuery = mock(TypedQuery.class);
    private final AppointmentRepositoryImpl repository = new AppointmentRepositoryImpl();

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(repository, "em", em);
        when(em.createQuery(anyString(), eq(Appointment.class))).thenReturn(dataQuery);
        when(em.createQuery(anyString(), eq(Long.class))).thenReturn(countQuery);
        when(dataQuery.setParameter(anyString(), any())).thenReturn(dataQuery);
        when(countQuery.setParameter(anyString(), any())).thenReturn(countQuery);
        when(dataQuery.setFirstResult(anyInt())).thenReturn(dataQuery);
        when(dataQuery.setMaxResults(anyInt())).thenReturn(dataQuery);
        when(dataQuery.getResultList()).thenReturn(List.of(Appointment.builder().appointmentId(UUID.randomUUID()).build()));
        when(countQuery.getSingleResult()).thenReturn(1L);
    }

    @Test
    void searchWithoutFiltersBuildsPagedResult() {
        var page = repository.search(null, null, null, null, PageRequest.of(2, 5));
        assertEquals(1, page.getContent().size());
        verify(dataQuery).setFirstResult(10);
        verify(dataQuery).setMaxResults(5);
        verify(dataQuery, never()).setParameter(anyString(), any());
    }

    @Test
    void searchAppliesEveryFilterAndEnumStatus() {
        UUID customer = UUID.randomUUID();
        LocalDateTime from = LocalDateTime.of(2026, 9, 1, 8, 0);
        LocalDateTime to = from.plusDays(2);
        repository.search(customer, "PENDING", from, to, PageRequest.of(0, 10));

        verify(dataQuery).setParameter("customerId", customer);
        verify(dataQuery).setParameter("status", vn.edu.fpt.cares.enums.AppointmentStatus.PENDING);
        verify(dataQuery).setParameter("from", from);
        verify(dataQuery).setParameter("to", to);
    }

    @ParameterizedTest
    @ValueSource(strings = {"upcoming", "completed", "cancelled", "checked_in", "unknown"})
    void customerSearchHandlesEverySupportedStatus(String status) {
        UUID customer = UUID.randomUUID();
        var page = repository.searchForCustomers(List.of(customer), "APPT-ABCD", "Nội khoa", status,
                LocalDateTime.of(2026, 9, 1, 0, 0), LocalDateTime.of(2026, 9, 30, 23, 59),
                PageRequest.of(1, 10));

        assertEquals(1, page.getContent().size());
        verify(dataQuery).setParameter("customerIds", List.of(customer));
        verify(dataQuery).setParameter("code", "%abcd%");
        verify(dataQuery).setParameter("specialty", "Nội khoa");
        verify(dataQuery).setParameter(eq("from"), any(LocalDateTime.class));
        verify(dataQuery).setParameter(eq("to"), any(LocalDateTime.class));
        if (status.equals("upcoming")) verify(dataQuery).setParameter(eq("statusList"), any());
        if (status.equals("completed")) verify(dataQuery).setParameter("visitStatus",
                vn.edu.fpt.cares.enums.VisitStatus.COMPLETED);
        if (status.equals("cancelled")) verify(dataQuery).setParameter("appStatus",
                vn.edu.fpt.cares.enums.AppointmentStatus.CANCELLED);
        if (status.equals("checked_in")) verify(dataQuery).setParameter("appStatus",
                vn.edu.fpt.cares.enums.AppointmentStatus.CHECKED_IN);
    }

    @Test
    void customerConvenienceSearchDelegatesWithSingleIdAndNoOptionalFilters() {
        UUID customer = UUID.randomUUID();
        var page = repository.searchForCustomer(customer, "", "", "", null, null, PageRequest.of(0, 10));
        assertEquals(1, page.getTotalElements());
        verify(dataQuery).setParameter("customerIds", List.of(customer));
        verify(dataQuery, never()).setParameter(eq("code"), any());
    }

    @Test
    void directCustomerAndGuestQueriesBindTheirIdentifiers() {
        UUID customer = UUID.randomUUID();
        when(dataQuery.getResultList()).thenReturn(List.of());
        assertTrue(repository.findByCustomerId(customer).isEmpty());
        verify(dataQuery).setParameter("customerId", customer);

        reset(dataQuery);
        when(em.createQuery(anyString(), eq(Appointment.class))).thenReturn(dataQuery);
        when(dataQuery.setParameter(anyString(), any())).thenReturn(dataQuery);
        when(dataQuery.getResultList()).thenReturn(List.of());
        assertTrue(repository.findGuestAppointmentsByPhone("0900000000").isEmpty());
        verify(dataQuery).setParameter("phone", "0900000000");
    }
}
