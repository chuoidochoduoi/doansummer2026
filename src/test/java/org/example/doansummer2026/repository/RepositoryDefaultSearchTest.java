package org.example.doansummer2026.repository;

import org.example.doansummer2026.enums.*;
import org.example.doansummer2026.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RepositoryDefaultSearchTest {

    private final PageRequest pageable = PageRequest.of(0, 10);

    @Test
    void medicalServiceSearchCoversEmptyAndEveryFilterCombination() {
        MedicalServiceRepository repository = mock(MedicalServiceRepository.class, CALLS_REAL_METHODS);
        Page<MedicalService> expected = new PageImpl<>(List.of());
        doReturn(expected).when(repository).findAll(any(Specification.class), eq(pageable));

        assertSame(expected, repository.search(null, null, null, null, pageable));
        assertSame(expected, repository.search("", null, null, null, pageable));
        assertSame(expected, repository.search("NỘI", DepartmentType.EXAMINATION,
                ServiceStatus.ACTIVE, UUID.randomUUID(), pageable));
        assertSame(expected, repository.searchCustomerBookable(null, null, pageable));
        assertSame(expected, repository.searchCustomerBookable(" ", null, pageable));
        assertSame(expected, repository.searchCustomerBookable("máu", DepartmentType.PARACLINICAL, pageable));
        verify(repository, times(6)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void auditLogSearchCoversNoFiltersAndAllFilters() {
        AuditLogRepository repository = mock(AuditLogRepository.class, CALLS_REAL_METHODS);
        Page<AuditLog> expected = new PageImpl<>(List.of());
        doReturn(expected).when(repository).findAll(any(Specification.class), eq(pageable));
        assertSame(expected, repository.search(null, null, null, null, null, pageable));
        assertSame(expected, repository.search(UUID.randomUUID(), AuditAction.UPDATE, "Profile",
                LocalDateTime.now().minusDays(1), LocalDateTime.now(), pageable));
    }

    @Test
    void icdSearchCoversBlankAndPopulatedFilters() {
        Icd10CodeRepository repository = mock(Icd10CodeRepository.class, CALLS_REAL_METHODS);
        Page<Icd10Code> expected = new PageImpl<>(List.of());
        doReturn(expected).when(repository).findAll(any(Specification.class), eq(pageable));
        assertSame(expected, repository.search(null, null, pageable));
        assertSame(expected, repository.search("", "", pageable));
        assertSame(expected, repository.search("J02", "RESPIRATORY", pageable));
    }

    @Test
    void staffScheduleSearchCoversNoFiltersAndAllFilters() {
        StaffScheduleRepository repository = mock(StaffScheduleRepository.class, CALLS_REAL_METHODS);
        Page<StaffSchedule> expected = new PageImpl<>(List.of());
        doReturn(expected).when(repository).findAll(any(Specification.class), eq(pageable));
        assertSame(expected, repository.search(null, null, null, null, pageable));
        assertSame(expected, repository.search(UUID.randomUUID(), LocalDate.now().minusDays(1),
                LocalDate.now(), ShiftConfig.builder().shiftId(UUID.randomUUID()).build(), pageable));
    }

    @Test
    void staffInfoSearchCoversBlankAndAllFilters() {
        StaffInfoRepository repository = mock(StaffInfoRepository.class, CALLS_REAL_METHODS);
        Page<StaffInfo> expected = new PageImpl<>(List.of());
        doReturn(expected).when(repository).findAll(any(Specification.class), eq(pageable));
        assertSame(expected, repository.search(null, null, null, pageable));
        assertSame(expected, repository.search("  ", null, null, pageable));
        assertSame(expected, repository.search(" bác sĩ ", UUID.randomUUID(), SystemRole.DOCTOR, pageable));
    }

    @Test
    void visitAndRecordSearchCoverNoFiltersAndAllFilters() {
        CustomerVisitRepository visits = mock(CustomerVisitRepository.class, CALLS_REAL_METHODS);
        Page<CustomerVisit> visitPage = new PageImpl<>(List.of());
        doReturn(visitPage).when(visits).findAll(any(Specification.class), eq(pageable));
        assertSame(visitPage, visits.search(null, null, null, null, pageable));
        assertSame(visitPage, visits.search(UUID.randomUUID(), VisitStatus.IN_PROGRESS,
                LocalDateTime.now().minusDays(1), LocalDateTime.now(), pageable));

        MedicalRecordRepository records = mock(MedicalRecordRepository.class, CALLS_REAL_METHODS);
        Page<MedicalRecord> recordPage = new PageImpl<>(List.of());
        doReturn(recordPage).when(records).findAll(any(Specification.class), eq(pageable));
        assertSame(recordPage, records.search(null, null, null, null, pageable));
        assertSame(recordPage, records.search(UUID.randomUUID(), MedicalRecordStatus.COMPLETED,
                LocalDateTime.now().minusDays(1), LocalDateTime.now(), pageable));
    }
}
