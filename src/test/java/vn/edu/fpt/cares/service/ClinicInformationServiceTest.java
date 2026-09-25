package vn.edu.fpt.cares.service;

import vn.edu.fpt.cares.dto.clinic.ClinicInformationRequest;
import vn.edu.fpt.cares.model.ClinicInformation;
import vn.edu.fpt.cares.repository.ClinicInformationRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClinicInformationServiceTest {

    private final ClinicInformationRepository repository = mock(ClinicInformationRepository.class);
    private final ClinicInformationService service = new ClinicInformationService(repository);

    @Test
    void getReturnsStoredValue() {
        ClinicInformation value = ClinicInformation.builder()
                .clinicInformationId(ClinicInformationService.SINGLETON_ID).clinicName("CareS").build();
        when(repository.findById(ClinicInformationService.SINGLETON_ID)).thenReturn(Optional.of(value));

        assertEquals("CareS", service.get().clinicName());
    }

    @Test
    void getCreatesCompleteDefaultWhenMissingWithoutPersistingIt() {
        when(repository.findById(ClinicInformationService.SINGLETON_ID)).thenReturn(Optional.empty());

        var response = service.get();

        assertEquals("Phòng khám CareS", response.clinicName());
        assertEquals("0101234567", response.taxCode());
        assertFalse(response.address().isBlank());
        verify(repository, never()).save(any());
    }

    @Test
    void updateNormalizesRequiredAndOptionalFields() {
        when(repository.findById(ClinicInformationService.SINGLETON_ID)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var request = new ClinicInformationRequest(
                "  CareS   Hòa Lạc ", " Công ty   CareS ", " 0101234567 ",
                "  GP   01 ", "   ", " SUPPORT@CARES.VN ", " 1900   1234 ",
                " Khu   CNC Hòa Lạc ", null, " https://facebook.test/c ", "", " https://zalo.test/c ",
                new BigDecimal("21.1"), new BigDecimal("105.5"));

        var response = service.update(request);

        assertEquals("CareS Hòa Lạc", response.clinicName());
        assertEquals("Công ty CareS", response.legalName());
        assertEquals("support@cares.vn", response.supportEmail());
        assertEquals("1900 1234", response.phone());
        assertEquals("Khu CNC Hòa Lạc", response.address());
        assertEquals("GP 01", response.operatingLicense());
        assertNull(response.shortDescription());
        assertNull(response.websiteUrl());
        assertNull(response.youtubeUrl());
        assertEquals("https://facebook.test/c", response.facebookUrl());
        assertEquals("https://zalo.test/c", response.zaloUrl());
        verify(repository).save(any(ClinicInformation.class));
    }

    @Test
    void updatePreservesExistingSingletonAndOptionalText() {
        ClinicInformation existing = ClinicInformation.builder()
                .clinicInformationId(ClinicInformationService.SINGLETON_ID).clinicName("Old").build();
        when(repository.findById(ClinicInformationService.SINGLETON_ID)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        var request = new ClinicInformationRequest(" New ", " Legal ", "0101234567", "GP",
                " Clinic description ", "mail@cares.vn", "0900000000", " Address ",
                "https://cares.vn", null, null, null, BigDecimal.ZERO, BigDecimal.ZERO);

        var response = service.update(request);

        assertSame(existing, existing);
        assertEquals("New", response.clinicName());
        assertEquals("Clinic description", response.shortDescription());
        assertEquals("https://cares.vn", response.websiteUrl());
    }
}
