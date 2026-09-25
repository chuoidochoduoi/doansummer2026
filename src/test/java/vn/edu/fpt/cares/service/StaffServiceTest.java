package vn.edu.fpt.cares.service;

import vn.edu.fpt.cares.dto.staff.*;
import vn.edu.fpt.cares.enums.*;
import vn.edu.fpt.cares.exception.*;
import vn.edu.fpt.cares.model.*;
import vn.edu.fpt.cares.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffServiceTest {
    @Mock StaffInfoRepository staffRepo;
    @Mock ProfileRepository profileRepo;
    @Mock AccountRepository accountRepo;
    @Mock DepartmentRepository departmentRepo;
    @Mock SpecializationService specializationService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock StaffCapabilityRepository staffCapabilityRepo;
    @Mock ServiceCapabilityRepository capabilityRepo;
    @InjectMocks StaffService service;

    StaffInfo staff(SystemRole role) {
        Account account = Account.builder().accountId(UUID.randomUUID()).username("staff.demo")
                .role(Role.STAFF).isActive(true).build();
        Profile profile = Profile.builder().profileId(UUID.randomUUID()).account(account)
                .fullName("Nguyễn Minh An").dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE).phone("0900000000").email("staff@example.test").build();
        StaffInfo staff = StaffInfo.builder().staffId(UUID.randomUUID()).profile(profile).systemRole(role).build();
        if (role.isDoctor()) staff.setSpecialization(Specialization.builder()
                .specializationId(UUID.randomUUID()).name("Nội khoa").build());
        return staff;
    }

    void lookup(StaffInfo staff) { when(staffRepo.findById(staff.getStaffId())).thenReturn(Optional.of(staff)); }
    void saveStaff() { when(staffRepo.save(any())).thenAnswer(call -> call.getArgument(0)); }
    StaffCreateRequest createRequest(SystemRole role, UUID specializationId) {
        return new StaffCreateRequest(" staff.demo ", "password-123", "Nguyễn Minh An", " 0900000000 ",
                " STAFF@EXAMPLE.TEST ", LocalDate.of(1990, 1, 1), "male", " Hà Nội ", " /avatar.png ",
                specializationId, role, "123456789012", " Đại học ", " Y Hà Nội ", "LICENSE-1");
    }

    @ParameterizedTest @EnumSource(SystemRole.class)
    void createNormalizesIdentityAndKeepsAccountStaffRole(SystemRole role) {
        UUID specializationId = role.isDoctor() ? UUID.randomUUID() : null;
        if (specializationId != null) when(specializationService.findActiveById(specializationId))
                .thenReturn(Specialization.builder().specializationId(specializationId).name("Nội khoa").build());
        when(passwordEncoder.encode("password-123")).thenReturn("hashed-password");
        when(accountRepo.save(any())).thenAnswer(call -> call.getArgument(0));
        when(profileRepo.save(any())).thenAnswer(call -> call.getArgument(0));
        saveStaff();
        StaffResponse response = service.create(createRequest(role, specializationId));
        ArgumentCaptor<Profile> profile = ArgumentCaptor.forClass(Profile.class);
        verify(profileRepo).save(profile.capture());
        assertEquals("staff.demo", profile.getValue().getAccount().getUsername());
        assertEquals("hashed-password", profile.getValue().getAccount().getPasswordHash());
        assertEquals(Role.STAFF, profile.getValue().getAccount().getRole());
        assertEquals("staff@example.test", profile.getValue().getEmail());
        assertEquals("0900000000", profile.getValue().getPhone());
        assertEquals("/avatar.png", profile.getValue().getAvatarUrl());
        assertEquals(role.normalized().name(), response.systemRole().name());
    }

    @ParameterizedTest @ValueSource(strings={"username", "nationalId", "license", "phone", "email", "specialization"})
    void createRejectsConflictsBeforeCreatingAccount(String conflict) {
        switch (conflict) {
            case "username" -> when(accountRepo.existsByUsername("staff.demo")).thenReturn(true);
            case "nationalId" -> when(staffRepo.existsByNationalId("123456789012")).thenReturn(true);
            case "license" -> when(staffRepo.existsByLicenseNumber("LICENSE-1")).thenReturn(true);
            case "phone" -> when(profileRepo.findFirstByPhone("0900000000")).thenReturn(Optional.of(new Profile()));
            case "email" -> when(profileRepo.findFirstByEmailIgnoreCase("staff@example.test")).thenReturn(Optional.of(new Profile()));
        }
        assertThrows(ConflictException.class, () -> service.create(createRequest(SystemRole.DOCTOR, null)));
        verify(accountRepo, never()).save(any());
        verify(staffRepo, never()).save(any());
    }

    @Test void updateChangesOnlyPermittedIdentityAndProfessionalFields() {
        StaffInfo staff = staff(SystemRole.DOCTOR); lookup(staff); saveStaff();
        when(accountRepo.findFirstByUsername("staff.demo")).thenReturn(Optional.of(staff.getProfile().getAccount()));
        when(profileRepo.findFirstByPhone("0900000000")).thenReturn(Optional.of(staff.getProfile()));
        when(profileRepo.findFirstByEmailIgnoreCase("staff@example.test")).thenReturn(Optional.of(staff.getProfile()));
        service.update(staff.getStaffId(), new StaffUpdateRequest(" staff.demo ", " Nguyễn   Minh Anh ",
                "0900000000", "STAFF@EXAMPLE.TEST", LocalDate.of(1991, 2, 3), "female", " Đà Nẵng ", " /new.png ",
                staff.getSpecialization().getSpecializationId(), SystemRole.DOCTOR, "NEW-ID", " Thạc sĩ ", " Trường Y ", "NEW-LICENSE"));
        assertEquals("Nguyễn Minh Anh", staff.getProfile().getFullName());
        assertEquals(Gender.FEMALE, staff.getProfile().getGender());
        assertEquals("staff@example.test", staff.getProfile().getEmail());
        assertEquals("Đà Nẵng", staff.getProfile().getAddress());
        assertEquals("Thạc sĩ", staff.getHighestDegree());
        assertEquals("NEW-LICENSE", staff.getLicenseNumber());
        verify(staffRepo).save(staff);
    }

    @ParameterizedTest @ValueSource(strings={"name", "digits", "birthMissing", "birthFuture", "gender", "contact", "role", "specialization", "doctorMissingSpecialization", "nationalId", "license", "username", "phone", "email"})
    void updateRejectsInvalidOrConflictingDataWithoutSaving(String condition) {
        StaffInfo staff = staff(SystemRole.DOCTOR); lookup(staff);
        StaffUpdateRequest request = mock(StaffUpdateRequest.class);
        switch(condition) {
            case "name" -> staff.getProfile().setFullName(" ");
            case "digits" -> when(request.fullName()).thenReturn("An 123");
            case "birthMissing" -> staff.getProfile().setDateOfBirth(null);
            case "birthFuture" -> when(request.dateOfBirth()).thenReturn(LocalDate.of(2999, 1, 1));
            case "gender" -> staff.getProfile().setGender(null);
            case "contact" -> { staff.getProfile().setEmail(null); staff.getProfile().setPhone(" "); }
            case "role" -> when(request.systemRole()).thenReturn(SystemRole.NURSE);
            case "specialization" -> when(request.specializationId()).thenReturn(UUID.randomUUID());
            case "doctorMissingSpecialization" -> staff.setSpecialization(null);
            case "nationalId" -> { when(request.nationalId()).thenReturn("duplicate"); when(staffRepo.existsByNationalId("duplicate")).thenReturn(true); }
            case "license" -> { when(request.licenseNumber()).thenReturn("duplicate"); when(staffRepo.existsByLicenseNumber("duplicate")).thenReturn(true); }
            case "username" -> { when(request.username()).thenReturn("other"); when(accountRepo.findFirstByUsername("other")).thenReturn(Optional.of(Account.builder().accountId(UUID.randomUUID()).build())); }
            case "phone" -> { when(request.phone()).thenReturn("0901111111"); when(profileRepo.findFirstByPhone("0901111111")).thenReturn(Optional.of(Profile.builder().profileId(UUID.randomUUID()).build())); }
            case "email" -> { when(request.email()).thenReturn("other@example.test"); when(profileRepo.findFirstByEmailIgnoreCase("other@example.test")).thenReturn(Optional.of(Profile.builder().profileId(UUID.randomUUID()).build())); }
        }
        RuntimeException error = assertThrows(RuntimeException.class, () -> service.update(staff.getStaffId(), request));
        assertTrue(error instanceof BadRequestException || error instanceof ConflictException);
        verify(staffRepo, never()).save(any());
    }

    @ParameterizedTest @ValueSource(strings={"OTHER", "invalid", " ", "female"})
    void updateValidatesGender(String gender) {
        StaffInfo staff = staff(SystemRole.NURSE); lookup(staff);
        StaffUpdateRequest request = mock(StaffUpdateRequest.class);
        when(request.gender()).thenReturn(gender);
        if (gender.equals("female")) {
            saveStaff(); service.update(staff.getStaffId(), request);
            assertEquals(Gender.FEMALE, staff.getProfile().getGender());
        } else {
            assertThrows(RuntimeException.class, () -> service.update(staff.getStaffId(), request));
            verify(staffRepo, never()).save(any());
        }
    }

    @Test void professionalUpdateTrimsAndClearsValues() {
        StaffInfo staff = staff(SystemRole.NURSE); lookup(staff); saveStaff();
        service.updateOwnProfessionalInfo(staff.getStaffId(), new StaffProfessionalUpdateRequest(" Cao đẳng ", " "));
        assertEquals("Cao đẳng", staff.getHighestDegree()); assertNull(staff.getUniversity());
    }

    @ParameterizedTest @EnumSource(SystemRole.class)
    void lockProtectsManagersAndDeactivatesOtherAccounts(SystemRole role) {
        StaffInfo staff = staff(role); lookup(staff);
        if (role == SystemRole.ADMIN || role == SystemRole.CLINIC_MANAGER) {
            assertThrows(ConflictException.class, () -> service.lock(staff.getStaffId()));
            verify(accountRepo, never()).save(any());
        } else {
            service.lock(staff.getStaffId());
            assertFalse(staff.getProfile().getAccount().getIsActive());
            verify(accountRepo).save(staff.getProfile().getAccount());
        }
    }

    @Test void lockCannotAbandonAssignedRoomOrOngoingWork() {
        StaffInfo staff = staff(SystemRole.DOCTOR); lookup(staff);
        when(staffRepo.countBlockingLockReferences(staff.getStaffId())).thenReturn(1L);
        assertThrows(ConflictException.class, () -> service.lock(staff.getStaffId()));
        assertTrue(staff.getProfile().getAccount().getIsActive()); verify(accountRepo, never()).save(any());
    }

    @Test void missingAndExistingStaffCannotBeDeleted() {
        UUID id = UUID.randomUUID();
        assertThrows(ResourceNotFoundException.class, () -> service.delete(id));
        when(staffRepo.existsById(id)).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.delete(id));
        verify(staffRepo, never()).deleteById(any());
    }

    @Test void capabilitiesAreDeduplicatedAndPersistedWithCertificateData() {
        StaffInfo staff = staff(SystemRole.DOCTOR); lookup(staff);
        UUID capabilityId = UUID.randomUUID();
        ServiceCapability capability = ServiceCapability.builder().capabilityId(capabilityId)
                .code("LAB").name("Xét nghiệm").active(true).build();
        when(capabilityRepo.findById(capabilityId)).thenReturn(Optional.of(capability));
        when(staffCapabilityRepo.saveAll(any())).thenAnswer(call -> call.getArgument(0));
        StaffCapabilityRequest first = new StaffCapabilityRequest(capabilityId, " CERT-1 ",
                LocalDate.of(2020, 1, 1), LocalDate.of(2030, 1, 1), " Bệnh viện ", null);
        StaffCapabilityRequest duplicate = new StaffCapabilityRequest(capabilityId, "CERT-2", null, null, null, null);
        StaffCapabilityRequest empty = new StaffCapabilityRequest(null, null, null, null, null, null);
        List<StaffCapabilityResponse> results = service.replaceCapabilities(staff.getStaffId(), List.of(first, duplicate, empty));
        assertEquals(1, results.size());
        assertEquals("CERT-1", results.get(0).certificateNumber());
        assertEquals(StaffCapabilityStatus.ACTIVE, results.get(0).status());
        assertEquals("Bệnh viện", results.get(0).issuingOrganization());
        assertEquals(first.expiryDate(), results.get(0).expiryDate());
        verify(staffCapabilityRepo).deleteAllByStaff_StaffId(staff.getStaffId());
        verify(capabilityRepo).findById(capabilityId);
    }

    @Test void nullCapabilitiesClearDoctorAssignments() {
        StaffInfo staff = staff(SystemRole.DOCTOR); lookup(staff);
        when(staffCapabilityRepo.saveAll(List.of())).thenReturn(List.of());
        assertTrue(service.replaceCapabilities(staff.getStaffId(), null).isEmpty());
        verify(staffCapabilityRepo).deleteAllByStaff_StaffId(staff.getStaffId());
        verifyNoInteractions(capabilityRepo);
    }

    @Test void nursesCannotReplaceTechnicalAssignments() {
        StaffInfo staff = staff(SystemRole.NURSE); lookup(staff);
        assertThrows(ConflictException.class, () -> service.replaceCapabilities(staff.getStaffId(), List.of()));
        verifyNoInteractions(staffCapabilityRepo, capabilityRepo);
    }

    @ParameterizedTest @ValueSource(booleans={false, true})
    void missingOrInactiveCapabilityCannotBeSaved(boolean exists) {
        StaffInfo staff = staff(SystemRole.DOCTOR); lookup(staff);
        UUID capabilityId = UUID.randomUUID();
        if (exists) when(capabilityRepo.findById(capabilityId)).thenReturn(Optional.of(
                ServiceCapability.builder().capabilityId(capabilityId).active(false).build()));
        RuntimeException error = assertThrows(RuntimeException.class, () -> service.replaceCapabilities(staff.getStaffId(),
                List.of(new StaffCapabilityRequest(capabilityId, null, null, null, null, null))));
        assertTrue(exists ? error instanceof ConflictException : error instanceof ResourceNotFoundException);
        verify(staffCapabilityRepo, never()).saveAll(any());
        // Deletion is within the facade transaction; Mockito does not establish rollback behavior.
    }

    @ParameterizedTest @EnumSource(StaffCapabilityStatus.class)
    void capabilityListingAndReplacementPreserveExplicitStatus(StaffCapabilityStatus status) {
        StaffInfo staff = staff(SystemRole.DOCTOR); lookup(staff);
        UUID id = UUID.randomUUID();
        ServiceCapability capability = ServiceCapability.builder().capabilityId(id).code("LAB").name("Lab").active(true).build();
        when(capabilityRepo.findById(id)).thenReturn(Optional.of(capability));
        when(staffCapabilityRepo.saveAll(any())).thenAnswer(call -> call.getArgument(0));
        assertEquals(status, service.replaceCapabilities(staff.getStaffId(), List.of(
                new StaffCapabilityRequest(id, null, null, null, null, status))).get(0).status());
        when(staffCapabilityRepo.findAllByStaff_StaffId(staff.getStaffId())).thenReturn(List.of(
                StaffCapability.builder().staff(staff).capability(capability).status(status).build()));
        assertEquals(id, service.listCapabilities(staff.getStaffId()).get(0).capabilityId());
    }

    @ParameterizedTest @ValueSource(strings={"all", "doctor", "nurse", "public", "headDoctors", "nurses"})
    void staffOptionsExcludeInactiveAndIncompleteAccounts(String mode) {
        StaffInfo active = staff(SystemRole.DOCTOR);
        StaffInfo inactive = staff(SystemRole.DOCTOR); inactive.getProfile().getAccount().setIsActive(false);
        StaffInfo noAccount = staff(SystemRole.DOCTOR); noAccount.getProfile().setAccount(null);
        StaffInfo noProfile = staff(SystemRole.DOCTOR); noProfile.setProfile(null);
        StaffInfo unknownActivity = staff(SystemRole.DOCTOR); unknownActivity.getProfile().getAccount().setIsActive(null);
        List<StaffInfo> values = List.of(inactive, noAccount, active, noProfile, unknownActivity);
        if (mode.equals("all")) when(staffRepo.findAll()).thenReturn(values);
        else when(staffRepo.findAllBySystemRoleIn(any())).thenReturn(values);
        UUID departmentId = UUID.randomUUID();
        active.setDepartment(Department.builder().departmentId(departmentId).build());
        ServiceCapability capability = ServiceCapability.builder().capabilityId(UUID.randomUUID()).build();
        StaffCapability valid = StaffCapability.builder().capability(capability).build();
        when(staffCapabilityRepo.findAllByStaff_StaffIdAndStatus(active.getStaffId(), StaffCapabilityStatus.ACTIVE))
                .thenReturn(List.of(valid, valid, new StaffCapability()));
        List<StaffOptionResponse> result = switch(mode) {
            case "all" -> service.listForSchedule(null);
            case "doctor" -> service.listForSchedule(SystemRole.DOCTOR);
            case "nurse" -> service.listForSchedule(SystemRole.NURSE);
            case "public" -> service.getPublicActiveDoctors();
            case "headDoctors" -> service.findAllDoctors();
            default -> service.findAllNurses();
        };
        assertEquals(1, result.size());
        assertEquals(active.getStaffId(), result.get(0).staffId());
        assertEquals(departmentId, result.get(0).assignedDepartmentId());
        assertEquals(List.of(capability.getCapabilityId()), result.get(0).capabilityIds());
        if (mode.equals("doctor")) verify(staffRepo).findAllBySystemRoleIn(List.of(SystemRole.DOCTOR, SystemRole.GENERAL_DOCTOR, SystemRole.SPECIALIST_DOCTOR));
        if (mode.equals("nurse") || mode.equals("nurses")) verify(staffRepo).findAllBySystemRoleIn(List.of(SystemRole.NURSE));
    }

    @Test void publicDoctorsSortBySpecializationThenName() {
        StaffInfo first = staff(SystemRole.DOCTOR); first.setSpecialization(null); first.getProfile().setFullName("An");
        StaffInfo second = staff(SystemRole.DOCTOR); second.getProfile().setFullName("Bình");
        StaffInfo third = staff(SystemRole.DOCTOR); third.getProfile().setFullName("Dung");
        when(staffRepo.findAllBySystemRoleIn(any())).thenReturn(List.of(third, second, first));
        assertEquals(List.of(first.getStaffId(), second.getStaffId(), third.getStaffId()),
                service.getPublicActiveDoctors().stream().map(StaffOptionResponse::staffId).toList());
    }

    @Test void searchPreservesPagingAndManagerProjection() {
        StaffInfo staff = staff(SystemRole.NURSE); lookup(staff);
        var pageable = org.springframework.data.domain.PageRequest.of(1, 10);
        var page = new org.springframework.data.domain.PageImpl<>(List.of(staff), pageable, 11);
        when(staffRepo.search("An", null, SystemRole.NURSE, pageable)).thenReturn(page);
        var result = service.search("An", null, SystemRole.NURSE, pageable);
        assertEquals(11, result.totalElements()); assertEquals(1, result.page());
        assertEquals(staff.getStaffId(), result.content().get(0).staffId());
        when(staffRepo.search("An", null, null, pageable)).thenReturn(page);
        assertEquals(11, service.searchForClinicManager("An", pageable).totalElements());
        assertNotNull(service.getForClinicManager(staff.getStaffId()));
    }

    @Test void lookupUsesExactStaffAndAccountIds() {
        StaffInfo staff = staff(SystemRole.NURSE); lookup(staff);
        UUID accountId = staff.getProfile().getAccount().getAccountId();
        when(staffRepo.findFirstByProfile_Account_AccountId(accountId)).thenReturn(Optional.of(staff));
        assertEquals(staff.getStaffId(), service.get(staff.getStaffId()).staffId());
        assertEquals(staff.getStaffId(), service.getByAccountId(accountId).staffId());
        assertThrows(ResourceNotFoundException.class, () -> service.get(UUID.randomUUID()));
        assertThrows(ResourceNotFoundException.class, () -> service.getByAccountId(UUID.randomUUID()));
    }

    @ParameterizedTest
    @CsvSource({"male,MALE", " FEMALE ,FEMALE"})
    void parseGenderAcceptsSupportedValues(String raw, Gender expected) {
        assertEquals(expected, ReflectionTestUtils.invokeMethod(service, "parseGender", raw));
    }

    @Test
    void parseGenderHandlesMissingOtherAndUnknownValues() {
        assertNull(ReflectionTestUtils.invokeMethod(service, "parseGender", (Object) null));
        assertNull(ReflectionTestUtils.invokeMethod(service, "parseGender", " "));
        assertThrows(ConflictException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "parseGender", "OTHER"));
        assertThrows(ConflictException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "parseGender", "unknown"));
    }
}
