package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.staff.*;
import org.example.doansummer2026.enums.*;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.*;
import org.example.doansummer2026.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffServiceTest {

    @Mock
    private StaffInfoRepository staffRepo;

    @Mock
    private ProfileRepository profileRepo;

    @Mock
    private AccountRepository accountRepo;

    @Mock
    private DepartmentRepository departmentRepo;

    @Mock
    private SpecializationService specializationService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private StaffCapabilityRepository staffCapabilityRepo;

    @Mock
    private ServiceCapabilityRepository capabilityRepo;

    @InjectMocks
    private StaffService staffService;


    // =========================================================
    // HELPER
    // =========================================================

    private StaffInfo staff(UUID staffId, SystemRole role) {

        Account account = Account.builder()
                .accountId(UUID.randomUUID())
                .username("staff01")
                .role(Role.STAFF)
                .isActive(true)
                .build();

        Profile profile = Profile.builder()
                .profileId(UUID.randomUUID())
                .account(account)
                .fullName("Nguyen Van Staff")
                .phone("0900000000")
                .build();

        return StaffInfo.builder()
                .staffId(staffId)
                .profile(profile)
                .systemRole(role)
                .build();
    }


    // =========================================================
    // FIND BY ID
    // =========================================================

    @Test
    void findById_ShouldReturn_WhenExists() {

        UUID id = UUID.randomUUID();

        StaffInfo staff =
                staff(id, SystemRole.NURSE);

        when(staffRepo.findById(id))
                .thenReturn(Optional.of(staff));

        assertSame(
                staff,
                staffService.findById(id)
        );
    }


    @Test
    void findById_ShouldThrow_WhenMissing() {

        UUID id = UUID.randomUUID();

        when(staffRepo.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> staffService.findById(id)
        );
    }


    // =========================================================
    // GET
    // =========================================================

    @Test
    void get_ShouldReturnResponse_WhenStaffExists() {

        UUID id = UUID.randomUUID();

        StaffInfo staff =
                staff(id, SystemRole.NURSE);

        when(staffRepo.findById(id))
                .thenReturn(Optional.of(staff));

        var result =
                staffService.get(id);

        assertNotNull(result);
    }


    // =========================================================
    // GET BY ACCOUNT
    // =========================================================

    @Test
    void getByAccountId_ShouldThrow_WhenMissing() {

        UUID accountId = UUID.randomUUID();

        when(
                staffRepo.findFirstByProfile_Account_AccountId(accountId)
        ).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> staffService.getByAccountId(accountId)
        );
    }


    @Test
    void getByAccountId_ShouldReturn_WhenFound() {

        UUID accountId = UUID.randomUUID();

        StaffInfo staff =
                staff(UUID.randomUUID(), SystemRole.NURSE);

        when(
                staffRepo.findFirstByProfile_Account_AccountId(accountId)
        ).thenReturn(Optional.of(staff));

        assertNotNull(
                staffService.getByAccountId(accountId)
        );
    }


    // =========================================================
    // CREATE - DUPLICATE USERNAME
    // =========================================================

    @Test
    void create_ShouldRejectDuplicateUsername() {

        StaffCreateRequest req =
                mock(StaffCreateRequest.class);

        when(req.systemRole())
                .thenReturn(SystemRole.NURSE);

        when(req.username())
                .thenReturn("nurse01");

        when(accountRepo.existsByUsername("nurse01"))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> staffService.create(req)
        );

        verify(accountRepo, never())
                .save(any());
    }


    // =========================================================
    // CREATE - DUPLICATE NATIONAL ID
    // =========================================================

    @Test
    void create_ShouldRejectDuplicateNationalId() {

        StaffCreateRequest req =
                mock(StaffCreateRequest.class);

        when(req.systemRole())
                .thenReturn(SystemRole.NURSE);

        when(req.username())
                .thenReturn("nurse01");

        when(req.nationalId())
                .thenReturn("001234567890");

        when(accountRepo.existsByUsername("nurse01"))
                .thenReturn(false);

        when(staffRepo.existsByNationalId("001234567890"))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> staffService.create(req)
        );
    }


    // =========================================================
    // CREATE - BLANK NATIONAL ID SHOULD SKIP CHECK
    // =========================================================

    @Test
    void create_ShouldIgnoreNationalIdDuplicateCheck_WhenBlank() {

        StaffCreateRequest req =
                mock(StaffCreateRequest.class);

        when(req.systemRole())
                .thenReturn(SystemRole.DOCTOR);

        when(req.username())
                .thenReturn("doctor01");

        when(req.nationalId())
                .thenReturn(" ");

        when(accountRepo.existsByUsername("doctor01"))
                .thenReturn(false);

        /*
         * Doctor không có specialization sẽ fail ở nhánh sau.
         * Mục tiêu ở đây là chứng minh nationalId blank không query repository.
         */
        assertThrows(
                ConflictException.class,
                () -> staffService.create(req)
        );

        verify(staffRepo, never())
                .existsByNationalId(anyString());
    }


    // =========================================================
    // CREATE - DUPLICATE LICENSE
    // =========================================================

    @Test
    void create_ShouldRejectDuplicateLicenseNumber() {

        StaffCreateRequest req =
                mock(StaffCreateRequest.class);

        when(req.systemRole())
                .thenReturn(SystemRole.NURSE);

        when(req.username())
                .thenReturn("staff01");

        when(req.licenseNumber())
                .thenReturn("LICENSE-001");

        when(accountRepo.existsByUsername("staff01"))
                .thenReturn(false);

        when(staffRepo.existsByLicenseNumber("LICENSE-001"))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> staffService.create(req)
        );
    }


    // =========================================================
    // CREATE - DUPLICATE PHONE
    // =========================================================

    @Test
    void create_ShouldRejectDuplicatePhone() {

        StaffCreateRequest req =
                mock(StaffCreateRequest.class);

        when(req.systemRole())
                .thenReturn(SystemRole.NURSE);

        when(req.username())
                .thenReturn("staff01");

        when(req.phone())
                .thenReturn("0901234567");

        when(accountRepo.existsByUsername("staff01"))
                .thenReturn(false);

        when(profileRepo.findFirstByPhone("0901234567"))
                .thenReturn(Optional.of(mock(Profile.class)));

        assertThrows(
                ConflictException.class,
                () -> staffService.create(req)
        );
    }


    // =========================================================
    // CREATE - DUPLICATE EMAIL
    // =========================================================

    @Test
    void create_ShouldRejectDuplicateEmail() {

        StaffCreateRequest req =
                mock(StaffCreateRequest.class);

        when(req.systemRole())
                .thenReturn(SystemRole.NURSE);

        when(req.username())
                .thenReturn("staff01");

        when(req.phone())
                .thenReturn("0901234567");

        when(req.email())
                .thenReturn("staff@test.com");

        when(accountRepo.existsByUsername("staff01"))
                .thenReturn(false);

        when(profileRepo.findFirstByPhone("0901234567"))
                .thenReturn(Optional.empty());

        when(profileRepo.findFirstByEmail("staff@test.com"))
                .thenReturn(Optional.of(mock(Profile.class)));

        assertThrows(
                ConflictException.class,
                () -> staffService.create(req)
        );
    }


    // =========================================================
    // CREATE - DOCTOR REQUIRES SPECIALIZATION
    // =========================================================

    @Test
    void create_ShouldRejectDoctorWithoutSpecialization() {

        StaffCreateRequest req =
                mock(StaffCreateRequest.class);

        when(req.systemRole())
                .thenReturn(SystemRole.DOCTOR);

        when(req.username())
                .thenReturn("doctor01");

        when(req.phone())
                .thenReturn("0901111111");

        when(req.email())
                .thenReturn("doctor@test.com");

        when(accountRepo.existsByUsername("doctor01"))
                .thenReturn(false);

        when(profileRepo.findFirstByPhone(any()))
                .thenReturn(Optional.empty());

        when(profileRepo.findFirstByEmail(any()))
                .thenReturn(Optional.empty());

        assertThrows(
                ConflictException.class,
                () -> staffService.create(req)
        );

        verify(accountRepo, never())
                .save(any());
    }


    // =========================================================
    // CREATE - INVALID GENDER
    // =========================================================

    @Test
    void create_ShouldRejectInvalidGender() {

        StaffCreateRequest req =
                mock(StaffCreateRequest.class);

        when(req.systemRole())
                .thenReturn(SystemRole.NURSE);

        when(req.username())
                .thenReturn("nurse02");

        when(req.password())
                .thenReturn("123456");

        when(req.phone())
                .thenReturn("0902222222");

        when(req.email())
                .thenReturn("nurse02@test.com");

        when(req.gender())
                .thenReturn("INVALID");

        when(accountRepo.existsByUsername(any()))
                .thenReturn(false);

        when(profileRepo.findFirstByPhone(any()))
                .thenReturn(Optional.empty());

        when(profileRepo.findFirstByEmail(any()))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode("123456"))
                .thenReturn("encoded");

        when(accountRepo.save(any(Account.class)))
                .thenAnswer(i -> i.getArgument(0));

        assertThrows(
                ConflictException.class,
                () -> staffService.create(req)
        );
    }


    // =========================================================
    // CREATE - GENDER OTHER IS FORBIDDEN
    // =========================================================

    @Test
    void create_ShouldRejectOtherGender() {

        StaffCreateRequest req =
                mock(StaffCreateRequest.class);

        when(req.systemRole())
                .thenReturn(SystemRole.NURSE);

        when(req.username())
                .thenReturn("nurse03");

        when(req.password())
                .thenReturn("123456");

        when(req.phone())
                .thenReturn("0903333333");

        when(req.email())
                .thenReturn("nurse03@test.com");

        when(req.gender())
                .thenReturn("OTHER");

        when(accountRepo.existsByUsername(any()))
                .thenReturn(false);

        when(profileRepo.findFirstByPhone(any()))
                .thenReturn(Optional.empty());

        when(profileRepo.findFirstByEmail(any()))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode(any()))
                .thenReturn("encoded");

        when(accountRepo.save(any(Account.class)))
                .thenAnswer(i -> i.getArgument(0));

        assertThrows(
                ConflictException.class,
                () -> staffService.create(req)
        );
    }


    // =========================================================
    // CREATE - SUCCESS NURSE
    // =========================================================

    @Test
    void create_ShouldCreateNurseSuccessfully() {

        StaffCreateRequest req =
                mock(StaffCreateRequest.class);

        when(req.systemRole())
                .thenReturn(SystemRole.NURSE);

        when(req.username())
                .thenReturn("nurse01");

        when(req.password())
                .thenReturn("123456");

        when(req.fullName())
                .thenReturn("Nguyen Thi Nurse");

        when(req.phone())
                .thenReturn("0901234567");

        when(req.email())
                .thenReturn("nurse@test.com");

        when(req.gender())
                .thenReturn("female");

        when(req.nationalId())
                .thenReturn(" 001122334455 ");

        when(req.highestDegree())
                .thenReturn(" Bachelor ");

        when(req.university())
                .thenReturn(" FPT ");

        when(accountRepo.existsByUsername("nurse01"))
                .thenReturn(false);

        when(staffRepo.existsByNationalId(anyString()))
                .thenReturn(false);

        when(profileRepo.findFirstByPhone(any()))
                .thenReturn(Optional.empty());

        when(profileRepo.findFirstByEmail(any()))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode("123456"))
                .thenReturn("encoded");

        when(accountRepo.save(any(Account.class)))
                .thenAnswer(i -> {
                    Account a = i.getArgument(0);
                    a.setAccountId(UUID.randomUUID());
                    return a;
                });

        when(profileRepo.save(any(Profile.class)))
                .thenAnswer(i -> {
                    Profile p = i.getArgument(0);
                    p.setProfileId(UUID.randomUUID());
                    return p;
                });

        when(staffRepo.save(any(StaffInfo.class)))
                .thenAnswer(i -> {
                    StaffInfo s = i.getArgument(0);
                    s.setStaffId(UUID.randomUUID());
                    return s;
                });

        var result =
                staffService.create(req);

        assertNotNull(result);

        verify(passwordEncoder)
                .encode("123456");

        verify(staffRepo)
                .save(argThat(s ->
                        s.getSystemRole() == SystemRole.NURSE
                                && "001122334455".equals(s.getNationalId())
                                && "Bachelor".equals(s.getHighestDegree())
                                && "FPT".equals(s.getUniversity())
                ));
    }


    // =========================================================
    // UPDATE - DUPLICATE NATIONAL ID
    // =========================================================

    @Test
    void update_ShouldRejectDuplicateNewNationalId() {

        UUID id = UUID.randomUUID();

        StaffInfo staff =
                staff(id, SystemRole.NURSE);

        staff.setNationalId("OLD");

        StaffUpdateRequest req =
                mock(StaffUpdateRequest.class);

        when(req.nationalId())
                .thenReturn("NEW");

        when(staffRepo.findById(id))
                .thenReturn(Optional.of(staff));

        when(staffRepo.existsByNationalId("NEW"))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> staffService.update(id, req)
        );
    }


    // =========================================================
    // UPDATE - DUPLICATE LICENSE
    // =========================================================

    @Test
    void update_ShouldRejectDuplicateNewLicense() {

        UUID id = UUID.randomUUID();

        StaffInfo staff =
                staff(id, SystemRole.NURSE);

        staff.setLicenseNumber("OLD");

        StaffUpdateRequest req =
                mock(StaffUpdateRequest.class);

        when(req.licenseNumber())
                .thenReturn("NEW-LICENSE");

        when(staffRepo.findById(id))
                .thenReturn(Optional.of(staff));

        when(staffRepo.existsByLicenseNumber("NEW-LICENSE"))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> staffService.update(id, req)
        );
    }


    // =========================================================
    // UPDATE - BECOMES DOCTOR WITHOUT SPECIALIZATION
    // =========================================================

    @Test
    void update_ShouldRejectDoctorWithoutSpecialization() {

        UUID id = UUID.randomUUID();

        StaffInfo staff =
                staff(id, SystemRole.NURSE);

        StaffUpdateRequest req =
                mock(StaffUpdateRequest.class);

        when(req.systemRole())
                .thenReturn(SystemRole.DOCTOR);

        when(staffRepo.findById(id))
                .thenReturn(Optional.of(staff));

        assertThrows(
                ConflictException.class,
                () -> staffService.update(id, req)
        );

        verify(staffRepo, never())
                .save(staff);
    }


    // =========================================================
    // UPDATE - SUCCESS BASIC FIELDS
    // =========================================================

    @Test
    void update_ShouldUpdateBasicFields() {

        UUID id = UUID.randomUUID();

        StaffInfo staff =
                staff(id, SystemRole.NURSE);

        StaffUpdateRequest req =
                mock(StaffUpdateRequest.class);

        when(req.username())
                .thenReturn("new.username");

        when(req.fullName())
                .thenReturn("New Full Name");

        when(req.phone())
                .thenReturn("0999999999");

        when(req.email())
                .thenReturn("new@test.com");

        when(req.gender())
                .thenReturn("male");

        when(req.address())
                .thenReturn("Ha Noi");

        when(req.bankAccount())
                .thenReturn("123456789");

        when(req.highestDegree())
                .thenReturn(" Master ");

        when(req.university())
                .thenReturn(" FPT University ");

        when(staffRepo.findById(id))
                .thenReturn(Optional.of(staff));

        when(staffRepo.save(staff))
                .thenReturn(staff);

        var result =
                staffService.update(id, req);

        assertNotNull(result);

        assertEquals(
                "new.username",
                staff.getProfile()
                        .getAccount()
                        .getUsername()
        );

        assertEquals(
                "New Full Name",
                staff.getProfile().getFullName()
        );

        assertEquals(
                Gender.MALE,
                staff.getProfile().getGender()
        );

        assertEquals(
                "Master",
                staff.getHighestDegree()
        );

        assertEquals(
                "FPT University",
                staff.getUniversity()
        );
    }


    // =========================================================
    // UPDATE - BLANK USERNAME IS IGNORED
    // =========================================================

    @Test
    void update_ShouldIgnoreBlankUsername() {

        UUID id = UUID.randomUUID();

        StaffInfo staff =
                staff(id, SystemRole.NURSE);

        String oldUsername =
                staff.getProfile()
                        .getAccount()
                        .getUsername();

        StaffUpdateRequest req =
                mock(StaffUpdateRequest.class);

        when(req.username())
                .thenReturn("   ");

        when(staffRepo.findById(id))
                .thenReturn(Optional.of(staff));

        when(staffRepo.save(staff))
                .thenReturn(staff);

        staffService.update(id, req);

        assertEquals(
                oldUsername,
                staff.getProfile()
                        .getAccount()
                        .getUsername()
        );
    }


    // =========================================================
    // UPDATE PROFESSIONAL INFO
    // =========================================================

    @Test
    void updateOwnProfessionalInfo_ShouldTrimValues() {

        UUID id = UUID.randomUUID();

        StaffInfo staff =
                staff(id, SystemRole.DOCTOR);

        StaffProfessionalUpdateRequest req =
                mock(StaffProfessionalUpdateRequest.class);

        when(req.highestDegree())
                .thenReturn(" CKI ");

        when(req.university())
                .thenReturn(" DH Y Ha Noi ");

        when(staffRepo.findById(id))
                .thenReturn(Optional.of(staff));

        when(staffRepo.save(staff))
                .thenReturn(staff);

        var result =
                staffService.updateOwnProfessionalInfo(
                        id,
                        req
                );

        assertNotNull(result);

        assertEquals(
                "CKI",
                staff.getHighestDegree()
        );

        assertEquals(
                "DH Y Ha Noi",
                staff.getUniversity()
        );
    }


    @Test
    void updateOwnProfessionalInfo_ShouldConvertBlankToNull() {

        UUID id = UUID.randomUUID();

        StaffInfo staff =
                staff(id, SystemRole.NURSE);

        StaffProfessionalUpdateRequest req =
                mock(StaffProfessionalUpdateRequest.class);

        when(req.highestDegree())
                .thenReturn(" ");

        when(req.university())
                .thenReturn(null);

        when(staffRepo.findById(id))
                .thenReturn(Optional.of(staff));

        when(staffRepo.save(staff))
                .thenReturn(staff);

        staffService.updateOwnProfessionalInfo(
                id,
                req
        );

        assertNull(
                staff.getHighestDegree()
        );

        assertNull(
                staff.getUniversity()
        );
    }


    // =========================================================
    // DELETE
    // =========================================================

    @Test
    void delete_ShouldThrow_WhenMissing() {

        UUID id = UUID.randomUUID();

        when(staffRepo.existsById(id))
                .thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> staffService.delete(id)
        );
    }


    @Test
    void delete_ShouldDelete_WhenExists() {

        UUID id = UUID.randomUUID();

        when(staffRepo.existsById(id))
                .thenReturn(true);

        staffService.delete(id);

        verify(staffRepo)
                .deleteById(id);
    }


    // =========================================================
    // LOCK
    // =========================================================

    @Test
    void lock_ShouldRejectAdmin() {

        UUID id = UUID.randomUUID();

        StaffInfo staff =
                staff(id, SystemRole.ADMIN);

        when(staffRepo.findById(id))
                .thenReturn(Optional.of(staff));

        assertThrows(
                ConflictException.class,
                () -> staffService.lock(id)
        );

        verifyNoInteractions(accountRepo);
    }


    @Test
    void lock_ShouldRejectClinicManager() {

        UUID id = UUID.randomUUID();

        StaffInfo staff =
                staff(id, SystemRole.CLINIC_MANAGER);

        when(staffRepo.findById(id))
                .thenReturn(Optional.of(staff));

        assertThrows(
                ConflictException.class,
                () -> staffService.lock(id)
        );
    }


    @Test
    void lock_ShouldDeactivateNormalStaffAccount() {

        UUID id = UUID.randomUUID();

        StaffInfo staff =
                staff(id, SystemRole.NURSE);

        Account account =
                staff.getProfile()
                        .getAccount();

        when(staffRepo.findById(id))
                .thenReturn(Optional.of(staff));

        when(accountRepo.save(account))
                .thenReturn(account);

        var result =
                staffService.lock(id);

        assertNotNull(result);

        assertFalse(
                account.getIsActive()
        );

        verify(accountRepo)
                .save(account);
    }


    // =========================================================
    // SEARCH
    // =========================================================

    @Test
    void search_ShouldReturnMappedPage() {

        UUID staffId = UUID.randomUUID();

        StaffInfo staff =
                staff(staffId, SystemRole.NURSE);

        var pageable =
                PageRequest.of(0, 10);

        when(
                staffRepo.search(
                        "abc",
                        null,
                        SystemRole.NURSE,
                        pageable
                )
        ).thenReturn(
                new PageImpl<>(List.of(staff))
        );

        var result =
                staffService.search(
                        "abc",
                        null,
                        SystemRole.NURSE,
                        pageable
                );

        assertNotNull(result);
    }


    // =========================================================
    // CAPABILITIES - LIST
    // =========================================================

    @Test
    void listCapabilities_ShouldThrow_WhenStaffMissing() {

        UUID id = UUID.randomUUID();

        when(staffRepo.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> staffService.listCapabilities(id)
        );
    }


    @Test
    void listCapabilities_ShouldReturnEmpty_WhenNoCapabilities() {

        UUID id = UUID.randomUUID();

        StaffInfo staff =
                staff(id, SystemRole.DOCTOR);

        when(staffRepo.findById(id))
                .thenReturn(Optional.of(staff));

        when(
                staffCapabilityRepo
                        .findAllByStaff_StaffId(id)
        ).thenReturn(List.of());

        var result =
                staffService.listCapabilities(id);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }


    // =========================================================
    // REPLACE CAPABILITIES - NON DOCTOR
    // =========================================================

    @Test
    void replaceCapabilities_ShouldRejectNonDoctor() {

        UUID id = UUID.randomUUID();

        StaffInfo staff =
                staff(id, SystemRole.NURSE);

        when(staffRepo.findById(id))
                .thenReturn(Optional.of(staff));

        assertThrows(
                ConflictException.class,
                () -> staffService.replaceCapabilities(
                        id,
                        List.of()
                )
        );

        verify(staffCapabilityRepo, never())
                .deleteAllByStaff_StaffId(id);
    }


    // =========================================================
    // REPLACE CAPABILITIES - NULL REQUEST
    // =========================================================

    @Test
    void replaceCapabilities_ShouldClearAll_WhenRequestsNull() {

        UUID id = UUID.randomUUID();

        StaffInfo staff =
                staff(id, SystemRole.DOCTOR);

        when(staffRepo.findById(id))
                .thenReturn(Optional.of(staff));

        when(staffCapabilityRepo.saveAll(any()))
                .thenReturn(List.of());

        var result =
                staffService.replaceCapabilities(
                        id,
                        null
                );

        assertTrue(result.isEmpty());

        verify(staffCapabilityRepo)
                .deleteAllByStaff_StaffId(id);
    }


    // =========================================================
    // REPLACE CAPABILITIES - NULL CAPABILITY IDs FILTERED
    // =========================================================

    @Test
    void replaceCapabilities_ShouldIgnoreRequestWithNullCapabilityId() {

        UUID id = UUID.randomUUID();

        StaffInfo staff =
                staff(id, SystemRole.DOCTOR);

        StaffCapabilityRequest req =
                mock(StaffCapabilityRequest.class);

        when(staffRepo.findById(id))
                .thenReturn(Optional.of(staff));

        when(staffCapabilityRepo.saveAll(any()))
                .thenReturn(List.of());

        var result =
                staffService.replaceCapabilities(
                        id,
                        List.of(req)
                );

        assertTrue(result.isEmpty());

        verifyNoInteractions(capabilityRepo);
    }


    // =========================================================
    // REPLACE CAPABILITIES - CAPABILITY MISSING
    // =========================================================

    @Test
    void replaceCapabilities_ShouldThrow_WhenCapabilityMissing() {

        UUID staffId = UUID.randomUUID();
        UUID capabilityId = UUID.randomUUID();

        StaffInfo staff =
                staff(staffId, SystemRole.DOCTOR);

        StaffCapabilityRequest req =
                mock(StaffCapabilityRequest.class);

        when(req.capabilityId())
                .thenReturn(capabilityId);

        when(staffRepo.findById(staffId))
                .thenReturn(Optional.of(staff));

        when(capabilityRepo.findById(capabilityId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> staffService.replaceCapabilities(
                        staffId,
                        List.of(req)
                )
        );
    }


    // =========================================================
    // REPLACE CAPABILITIES - REMOVE DUPLICATES
    // =========================================================

    @Test
    void replaceCapabilities_ShouldRemoveDuplicateCapabilityIds() {

        UUID staffId = UUID.randomUUID();
        UUID capabilityId = UUID.randomUUID();

        StaffInfo staff =
                staff(staffId, SystemRole.DOCTOR);

        StaffCapabilityRequest first =
                mock(StaffCapabilityRequest.class);

        StaffCapabilityRequest duplicate =
                mock(StaffCapabilityRequest.class);

        when(first.capabilityId())
                .thenReturn(capabilityId);

        when(duplicate.capabilityId())
                .thenReturn(capabilityId);

        var capability =
                mock(
                        org.example.doansummer2026.model.ServiceCapability.class
                );

        when(staffRepo.findById(staffId))
                .thenReturn(Optional.of(staff));

        when(capabilityRepo.findById(capabilityId))
                .thenReturn(Optional.of(capability));

        when(staffCapabilityRepo.saveAll(any()))
                .thenAnswer(i -> {
                    Iterable<StaffCapability> iterable =
                            i.getArgument(0);

                    List<StaffCapability> result =
                            new ArrayList<>();

                    iterable.forEach(result::add);

                    return result;
                });

        var result =
                staffService.replaceCapabilities(
                        staffId,
                        List.of(
                                first,
                                duplicate
                        )
                );

        assertEquals(
                1,
                result.size()
        );

        verify(capabilityRepo, times(1))
                .findById(capabilityId);
    }


    // =========================================================
    // REPLACE CAPABILITIES - DEFAULT ACTIVE STATUS
    // =========================================================

    @Test
    void replaceCapabilities_ShouldUseActiveStatus_WhenStatusNull() {

        UUID staffId = UUID.randomUUID();
        UUID capabilityId = UUID.randomUUID();

        StaffInfo staff =
                staff(staffId, SystemRole.DOCTOR);

        StaffCapabilityRequest req =
                mock(StaffCapabilityRequest.class);

        when(req.capabilityId())
                .thenReturn(capabilityId);

        when(req.certificateNumber())
                .thenReturn(" CERT-01 ");

        when(req.issuingOrganization())
                .thenReturn(" Hospital ");

        var capability =
                mock(
                        org.example.doansummer2026.model.ServiceCapability.class
                );

        when(staffRepo.findById(staffId))
                .thenReturn(Optional.of(staff));

        when(capabilityRepo.findById(capabilityId))
                .thenReturn(Optional.of(capability));

        when(staffCapabilityRepo.saveAll(any()))
                .thenAnswer(i -> {
                    Iterable<StaffCapability> iterable =
                            i.getArgument(0);

                    List<StaffCapability> list =
                            new ArrayList<>();

                    iterable.forEach(list::add);

                    return list;
                });

        var result =
                staffService.replaceCapabilities(
                        staffId,
                        List.of(req)
                );

        assertEquals(1, result.size());

        verify(staffCapabilityRepo)
                .saveAll(argThat(iterable -> {

                    StaffCapability value =
                            iterable.iterator().next();

                    return value.getStatus()
                            == StaffCapabilityStatus.ACTIVE
                            &&
                            "CERT-01".equals(
                                    value.getCertificateNumber()
                            )
                            &&
                            "Hospital".equals(
                                    value.getIssuingOrganization()
                            );
                }));
    }


    // =========================================================
    // LIST FOR SCHEDULE
    // =========================================================

    @Test
    void listForSchedule_ShouldLoadAll_WhenRoleNull() {

        when(staffRepo.findAll())
                .thenReturn(List.of());

        var result =
                staffService.listForSchedule(null);

        assertTrue(result.isEmpty());

        verify(staffRepo)
                .findAll();
    }


    @Test
    void listForSchedule_ShouldUseSingleRole_WhenNotDoctor() {

        when(
                staffRepo.findAllBySystemRoleIn(
                        List.of(SystemRole.NURSE)
                )
        ).thenReturn(List.of());

        staffService.listForSchedule(
                SystemRole.NURSE
        );

        verify(staffRepo)
                .findAllBySystemRoleIn(
                        List.of(SystemRole.NURSE)
                );
    }


    @Test
    void listForSchedule_ShouldNormalizeDoctorRoles() {

        when(
                staffRepo.findAllBySystemRoleIn(anyList())
        ).thenReturn(List.of());

        staffService.listForSchedule(
                SystemRole.DOCTOR
        );

        verify(staffRepo)
                .findAllBySystemRoleIn(
                        argThat(roles ->
                                roles.contains(SystemRole.DOCTOR)
                                        && roles.contains(SystemRole.GENERAL_DOCTOR)
                                        && roles.contains(SystemRole.SPECIALIST_DOCTOR)
                        )
                );
    }


    // =========================================================
    // FIND ALL DOCTORS
    // =========================================================

    @Test
    void findAllDoctors_ShouldReturnEmpty_WhenNoDoctors() {

        when(
                staffRepo.findAllBySystemRoleIn(anyList())
        ).thenReturn(List.of());

        when(departmentRepo.findAll())
                .thenReturn(List.of());

        var result =
                staffService.findAllDoctors();

        assertTrue(result.isEmpty());
    }


    @Test
    void findAllDoctors_ShouldMapHeadDoctorDepartment() {

        UUID doctorId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();

        StaffInfo doctor =
                staff(doctorId, SystemRole.DOCTOR);

        Department department =
                Department.builder()
                        .departmentId(departmentId)
                        .headDoctor(doctor)
                        .build();

        when(
                staffRepo.findAllBySystemRoleIn(anyList())
        ).thenReturn(List.of(doctor));

        when(departmentRepo.findAll())
                .thenReturn(List.of(department));

        var result =
                staffService.findAllDoctors();

        assertEquals(
                1,
                result.size()
        );
    }


    // =========================================================
    // FIND ALL NURSES
    // =========================================================

    @Test
    void findAllNurses_ShouldHandleNurseWithoutDepartment() {

        StaffInfo nurse =
                staff(
                        UUID.randomUUID(),
                        SystemRole.NURSE
                );

        when(
                staffRepo.findAllBySystemRoleIn(
                        List.of(SystemRole.NURSE)
                )
        ).thenReturn(List.of(nurse));

        var result =
                staffService.findAllNurses();

        assertEquals(
                1,
                result.size()
        );
    }


    @Test
    void findAllNurses_ShouldIncludeDepartment_WhenAssigned() {

        UUID departmentId = UUID.randomUUID();

        StaffInfo nurse =
                staff(
                        UUID.randomUUID(),
                        SystemRole.NURSE
                );

        Department department =
                Department.builder()
                        .departmentId(departmentId)
                        .build();

        nurse.setDepartment(department);

        when(
                staffRepo.findAllBySystemRoleIn(
                        List.of(SystemRole.NURSE)
                )
        ).thenReturn(List.of(nurse));

        var result =
                staffService.findAllNurses();

        assertEquals(
                1,
                result.size()
        );
    }
}