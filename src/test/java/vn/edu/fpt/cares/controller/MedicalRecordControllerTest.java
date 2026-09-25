package vn.edu.fpt.cares.controller;

import vn.edu.fpt.cares.common.PageResponse;
import vn.edu.fpt.cares.dto.appointment.AppointmentCreateRequest;
import vn.edu.fpt.cares.dto.clinicalform.ResolvedClinicalFormResponse;
import vn.edu.fpt.cares.dto.medicalhistory.*;
import vn.edu.fpt.cares.dto.medicalrecord.*;
import vn.edu.fpt.cares.dto.profile.ProfileResponse;
import vn.edu.fpt.cares.dto.profile.ProfileUpdateRequest;
import vn.edu.fpt.cares.enums.BloodType;
import vn.edu.fpt.cares.enums.MedicalRecordStatus;
import vn.edu.fpt.cares.enums.SystemRole;
import vn.edu.fpt.cares.exception.BadRequestException;
import vn.edu.fpt.cares.exception.ResourceNotFoundException;
import vn.edu.fpt.cares.model.Account;
import vn.edu.fpt.cares.model.Profile;
import vn.edu.fpt.cares.service.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicalRecordControllerTest {
    @Mock MedicalRecordService service;
    @Mock AuthService authService;
    @Mock AppointmentService appointmentService;
    @Mock ProfileService profileService;
    @Mock FamilyAccessService familyAccessService;
    private MedicalRecordController controller;
    private UUID accountId;

    @BeforeEach
    void setUp() {
        controller = new MedicalRecordController(service, authService, appointmentService, profileService,
                familyAccessService);
        accountId = UUID.randomUUID();
        lenient().when(authService.currentAccount()).thenReturn(Account.builder().accountId(accountId).build());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void staffReadEndpointsDelegate() {
        UUID id = UUID.randomUUID();
        var pageable = PageRequest.of(0, 10);
        PageResponse<MedicalRecordResponse> page = page();
        LocalDateTime from = LocalDateTime.of(2026, 9, 5, 8, 0), to = from.plusHours(8);
        when(service.search(id, MedicalRecordStatus.COMPLETED, from, to, pageable)).thenReturn(page);
        assertSame(page, controller.list(id, MedicalRecordStatus.COMPLETED, from, to, pageable).getBody());

        MedicalRecordResponse record = record(id);
        ResolvedClinicalFormResponse form = mock(ResolvedClinicalFormResponse.class);
        PatientAllergyResponse allergy = mock(PatientAllergyResponse.class);
        VisitDetailResponse detail = mock(VisitDetailResponse.class);
        when(service.get(id)).thenReturn(record);
        when(service.getClinicalForm(id)).thenReturn(form);
        when(service.getPatientAllergies(id)).thenReturn(allergy);
        when(service.getVisitDetailForStaff(id)).thenReturn(detail);
        when(service.getPreviousHistoryForDoctor(id)).thenReturn(List.of(mock(MedicalHistoryResponse.class)));
        assertSame(record, controller.get(id).getBody());
        assertSame(form, controller.clinicalForm(id).getBody());
        assertSame(allergy, controller.patientAllergies(id).getBody());
        assertSame(detail, controller.staffVisitDetail(id).getBody());
        assertEquals(1, controller.previousHistory(id).getBody().size());

        PatientAllergyRequest request = mock(PatientAllergyRequest.class);
        when(service.updatePatientAllergies(id, request)).thenReturn(allergy);
        assertSame(allergy, controller.updatePatientAllergies(id, request).getBody());
    }

    @Test
    void createEnforcesDoctorOwnershipButAdminMayCreate() {
        UUID doctorId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        MedicalRecordCreateRequest request = new MedicalRecordCreateRequest(UUID.randomUUID(), doctorId,
                null, null, null, null, null, null, null);
        MedicalRecordResponse response = record(id);
        when(authService.getCurrentSystemRole()).thenReturn(SystemRole.DOCTOR);
        when(authService.currentStaffId()).thenReturn(UUID.randomUUID());
        assertThrows(BadRequestException.class, () -> controller.create(request));

        when(authService.currentStaffId()).thenReturn(doctorId);
        when(service.create(request)).thenReturn(response);
        assertEquals(201, controller.create(request).getStatusCode().value());

        when(authService.getCurrentSystemRole()).thenReturn(SystemRole.ADMIN);
        assertEquals(201, controller.create(request).getStatusCode().value());
    }

    @Test
    void staffMutationEndpointsDelegate() {
        UUID id = UUID.randomUUID();
        MedicalRecordUpdateRequest update = mock(MedicalRecordUpdateRequest.class);
        MedicalRecordResponse response = record(id);
        when(service.update(id, update)).thenReturn(response);
        when(service.saveDraft(id, update)).thenReturn(response);
        when(service.complete(id, update)).thenReturn(response);
        when(service.rate(id, 5)).thenReturn(response);
        assertSame(response, controller.update(id, update).getBody());
        assertSame(response, controller.saveDraft(id, update).getBody());
        assertSame(response, controller.complete(id, update).getBody());
        assertSame(response, controller.rate(id, 5).getBody());
        assertEquals(204, controller.delete(id).getStatusCode().value());
        verify(service).delete(id);

        AppointmentCreateRequest appointment = mock(AppointmentCreateRequest.class);
        FollowUpResponse followUp = mock(FollowUpResponse.class);
        when(service.scheduleFollowUp(id, appointment)).thenReturn(followUp);
        assertSame(followUp, controller.createFollowUpAppointment(id, appointment).getBody());
    }

    @Test
    void patientHistoryReturnsDataForReadableProfileAndEmptyForNullProfileId() {
        UUID profileId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        var pageable = PageRequest.of(0, 10);
        Profile profile = Profile.builder().profileId(profileId).build();
        when(familyAccessService.resolveReadableProfile(accountId, profileId)).thenReturn(profile);
        PageResponse<MedicalHistoryResponse> history = page();
        PageResponse<VisitHistorySummaryResponse> visits = page();
        when(service.getMedicalHistoryForPatient(profileId, "nội", pageable)).thenReturn(history);
        when(service.getVisitHistoryForPatient(profileId, "nội", pageable)).thenReturn(visits);
        assertEquals(0, controller.getMedicalHistory("nội", profileId, pageable).getBody().total());
        assertEquals(0, controller.getVisitHistory("nội", profileId, pageable).getBody().total());

        Profile noId = Profile.builder().profileId(null).build();
        when(familyAccessService.resolveReadableProfile(accountId, null)).thenReturn(noId);
        assertTrue(controller.getMedicalHistory(null, null, pageable).getBody().items().isEmpty());
        assertTrue(controller.getVisitHistory(null, null, pageable).getBody().items().isEmpty());
        assertThrows(ResourceNotFoundException.class, () -> controller.getVisitDetail(recordId, null));
        assertThrows(ResourceNotFoundException.class, () -> controller.getPatientVisitDetail(recordId, null));
    }

    @Test
    void patientDetailClinicalFormRatingAndFeedbackAreOwnershipScoped() {
        UUID profileId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        Profile profile = Profile.builder().profileId(profileId).build();
        when(familyAccessService.resolveReadableProfile(accountId, profileId)).thenReturn(profile);
        when(familyAccessService.resolveActiveProfile(accountId, profileId)).thenReturn(profile);
        VisitDetailResponse detail = mock(VisitDetailResponse.class);
        ResolvedClinicalFormResponse form = mock(ResolvedClinicalFormResponse.class);
        MedicalRecordResponse record = record(recordId);
        FeedbackResponse feedback = mock(FeedbackResponse.class);
        FeedbackRequest feedbackRequest = mock(FeedbackRequest.class);
        when(service.getVisitDetailByRecordId(recordId, profileId)).thenReturn(detail);
        when(service.getPatientVisitDetail(recordId, profileId)).thenReturn(detail);
        when(service.getClinicalFormForPatient(recordId, profileId)).thenReturn(form);
        when(service.rate(recordId, 4)).thenReturn(record);
        when(service.submitFeedback(recordId, profileId, feedbackRequest)).thenReturn(feedback);
        assertSame(detail, controller.getVisitDetail(recordId, profileId).getBody());
        assertSame(detail, controller.getPatientVisitDetail(recordId, profileId).getBody());
        assertSame(form, controller.getPatientClinicalForm(recordId, profileId).getBody());
        assertSame(record, controller.rateVisit(recordId, profileId, 4).getBody());
        verify(service, times(2)).getVisitDetailByRecordId(recordId, profileId);
        assertSame(feedback, controller.submitFeedback(recordId, profileId, feedbackRequest).getBody());

        Profile noId = Profile.builder().profileId(null).build();
        when(familyAccessService.resolveActiveProfile(accountId, null)).thenReturn(noId);
        assertThrows(ResourceNotFoundException.class, () -> controller.rateVisit(recordId, null, 4));
    }

    @Test
    void feedbackManagementEndpointsDelegate() {
        var pageable = PageRequest.of(0, 10);
        PageResponse<FeedbackResponse> page = page();
        when(service.listFeedbacks(null, pageable)).thenReturn(page);
        when(service.countUnansweredFeedbacks()).thenReturn(3L);
        assertSame(page, controller.feedbacks(pageable).getBody());
        assertEquals(3L, controller.countUnansweredFeedbacks().getBody().get("count"));

        UUID id = UUID.randomUUID(), staffId = UUID.randomUUID();
        FeedbackResponse response = mock(FeedbackResponse.class);
        when(authService.currentStaffId()).thenReturn(staffId);
        when(service.respondFeedback(id, staffId, "Đã liên hệ")).thenReturn(response);
        assertSame(response, controller.respond(id, Map.of("response", "Đã liên hệ")).getBody());
    }

    @Test
    void receptionistCustomerAndVisitEndpointsDelegate() {
        UUID customerId = UUID.randomUUID(), visitId = UUID.randomUUID();
        var pageable = PageRequest.of(0, 10);
        PageResponse<ReceptionistRecordResponse> records = page();
        PageResponse<ReceptionistCustomerResponse> customers = page();
        PageResponse<MedicalHistoryResponse> visits = page();
        when(service.searchForReceptionist("an", "MALE", "20-30", BloodType.A_POSITIVE, pageable)).thenReturn(records);
        when(service.searchUniqueCustomers("an", "MALE", "20-30", BloodType.A_POSITIVE, pageable)).thenReturn(customers);
        when(service.getMedicalHistoryForPatient(customerId, "nội", pageable)).thenReturn(visits);
        assertEquals(0, controller.listRecordsForReceptionist("an", "MALE", "20-30", BloodType.A_POSITIVE,
                pageable).getBody().total());
        assertSame(customers, controller.listCustomersForReceptionist("an", "MALE", "20-30",
                BloodType.A_POSITIVE, pageable).getBody());
        assertSame(customers, controller.listPatientsForClinicManager("an", "MALE", "20-30",
                BloodType.A_POSITIVE, pageable).getBody());
        assertSame(visits, controller.getCustomerVisitsForReceptionist(customerId, "nội", pageable).getBody());

        ReceptionistCustomerResponse customer = mock(ReceptionistCustomerResponse.class);
        VisitDetailResponse detail = mock(VisitDetailResponse.class);
        when(service.getCustomerForReceptionist(customerId)).thenReturn(customer);
        when(service.getVisitDetail(visitId, customerId)).thenReturn(detail);
        when(service.searchByPhone("0987654321")).thenReturn(List.of(mock(ReceptionistAllCustomerResponse.class)));
        assertSame(customer, controller.getCustomerForReceptionist(customerId).getBody());
        assertSame(detail, controller.getCustomerVisitForReceptionist(customerId, visitId).getBody());
        assertEquals(1, controller.searchByPhoneForReceptionist("0987654321").getBody().size());

        ProfileUpdateRequest update = mock(ProfileUpdateRequest.class);
        ProfileResponse updated = mock(ProfileResponse.class);
        when(profileService.update(customerId, update)).thenReturn(updated);
        assertSame(updated, controller.updateCustomerForReceptionist(customerId, update).getBody());
    }

    private MedicalRecordResponse record(UUID id) {
        MedicalRecordResponse response = mock(MedicalRecordResponse.class);
        lenient().when(response.recordId()).thenReturn(id);
        return response;
    }

    private <T> PageResponse<T> page() {
        return new PageResponse<>(List.of(), 0, 10, 0, 0, true, true);
    }
}
