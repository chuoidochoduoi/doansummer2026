package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.family.FamilyMemberRequest;
import org.example.doansummer2026.enums.*;
import org.example.doansummer2026.exception.*;
import org.example.doansummer2026.model.*;
import org.example.doansummer2026.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FamilyMemberServiceTest {
    @Mock FamilyAccessService access;
    @Mock FamilyMemberRepository members;
    @Mock ProfileRepository profiles;
    @Mock AppointmentRepository appointments;
    @Mock CustomerVisitRepository visits;
    @InjectMocks FamilyMemberService service;

    private final UUID accountId = UUID.randomUUID();
    private final Profile owner = Profile.builder().profileId(UUID.randomUUID()).fullName("Nguyễn Anh Đức")
            .dateOfBirth(LocalDate.of(1980, 1, 1)).gender(Gender.MALE)
            .phone("0900000000").email("owner@example.test").build();
    private final Profile child = Profile.builder().profileId(UUID.randomUUID()).fullName("Nguyễn Minh An")
            .dateOfBirth(LocalDate.of(2010, 2, 3)).gender(Gender.FEMALE).build();
    private final FamilyMember relation = FamilyMember.builder().familyMemberId(UUID.randomUUID())
            .ownerProfile(owner).memberProfile(child).relationship(FamilyRelationship.CHILD).isActive(true).build();

    private FamilyMemberRequest request(AllergyStatus status, List<String> allergies) {
        return new FamilyMemberRequest("  Nguyễn   Minh An  ", child.getDateOfBirth(), Gender.FEMALE,
                FamilyRelationship.CHILD, "  Hà   Nội ", null, status, allergies, true);
    }

    private void ownerExists() {
        when(access.ownerProfile(accountId)).thenReturn(owner);
    }

    private void ownedRelation() {
        ownerExists();
        when(members.findByFamilyMemberIdAndOwnerProfile_ProfileId(relation.getFamilyMemberId(), owner.getProfileId()))
                .thenReturn(Optional.of(relation));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void listUsesRequestedActiveScopeAndOwnerContact(boolean includeInactive) {
        ownerExists();
        if (includeInactive) when(members.findAllByOwnerProfile_ProfileIdOrderByCreatedAtAsc(owner.getProfileId()))
                .thenReturn(List.of(relation));
        else when(members.findAllByOwnerProfile_ProfileIdAndIsActiveTrueOrderByCreatedAtAsc(owner.getProfileId()))
                .thenReturn(List.of(relation));
        var result = service.list(accountId, includeInactive);
        assertEquals(1, result.size());
        assertEquals(child.getProfileId(), result.get(0).patientProfileId());
        assertEquals(owner.getPhone(), result.get(0).contactPhone());
        assertEquals(owner.getEmail(), result.get(0).contactEmail());
        assertEquals("Con", result.get(0).relationshipName());
    }

    static Stream<Arguments> allergyCases() {
        return Stream.of(
                Arguments.of(null, List.of(), null),
                Arguments.of(null, List.of(" Penicillin "), "Penicillin"),
                Arguments.of(AllergyStatus.UNVERIFIED, List.of("Penicillin"), null),
                Arguments.of(AllergyStatus.NONE_REPORTED, List.of(), ""),
                Arguments.of(AllergyStatus.REPORTED, List.of(" Penicillin ", "Phấn hoa"), "Penicillin\nPhấn hoa"));
    }

    @ParameterizedTest
    @MethodSource("allergyCases")
    void createStoresNormalizedProfileAndSeparateOwnedRelation(AllergyStatus status, List<String> allergies, String stored) {
        ownerExists();
        when(profiles.save(any())).thenAnswer(inv -> {
            Profile profile = inv.getArgument(0);
            profile.setProfileId(child.getProfileId());
            return profile;
        });
        when(members.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var result = service.create(accountId, request(status, allergies));
        ArgumentCaptor<Profile> saved = ArgumentCaptor.forClass(Profile.class);
        verify(profiles).save(saved.capture());
        assertEquals("Nguyễn Minh An", saved.getValue().getFullName());
        assertEquals("Hà Nội", saved.getValue().getAddress());
        assertEquals(stored, saved.getValue().getAllergies());
        assertNull(saved.getValue().getAccount());
        assertTrue(result.active());
        verify(members).save(argThat(member -> member.getOwnerProfile() == owner
                && member.getMemberProfile() == saved.getValue()
                && member.getRelationship() == FamilyRelationship.CHILD));
    }

    @ParameterizedTest
    @NullSource
    @EnumSource(value = Gender.class, names = "OTHER")
    void unsupportedGenderIsRejectedBeforeLookup(Gender gender) {
        var req = new FamilyMemberRequest("Nguyễn An", child.getDateOfBirth(), gender,
                FamilyRelationship.CHILD, null, null, null, null, true);
        assertThrows(BadRequestException.class, () -> service.create(accountId, req));
        verifyNoInteractions(access, profiles, members);
    }

    @Test
    void reportedAllergyNeedsAtLeastOneItem() {
        ownerExists();
        assertThrows(BadRequestException.class,
                () -> service.create(accountId, request(AllergyStatus.REPORTED, List.of(" "))));
        verify(profiles, never()).save(any());
        verify(members, never()).save(any());
    }

    @Test
    void duplicateInactiveMemberIsRejectedDespiteNameCaseAndSpacing() {
        ownerExists();
        child.setFullName("  NGUYỄN MINH   AN ");
        relation.setIsActive(false);
        when(members.findAllByOwnerProfile_ProfileIdOrderByCreatedAtAsc(owner.getProfileId())).thenReturn(List.of(relation));
        assertThrows(ConflictException.class, () -> service.create(accountId, request(null, null)));
        verifyNoInteractions(profiles);
        verify(members, never()).save(any());
    }

    @Test
    void ownerCannotBeAddedAsOwnDependent() {
        ownerExists();
        var req = new FamilyMemberRequest(" Nguyễn Anh Đức ", owner.getDateOfBirth(), owner.getGender(),
                FamilyRelationship.OTHER, null, null, null, null, true);
        assertThrows(ConflictException.class, () -> service.create(accountId, req));
        verifyNoInteractions(profiles);
    }

    @Test
    void updateExcludesCurrentProfileFromDuplicateCheck() {
        ownedRelation();
        when(members.findAllByOwnerProfile_ProfileIdOrderByCreatedAtAsc(owner.getProfileId())).thenReturn(List.of(relation));
        when(members.save(relation)).thenReturn(relation);
        var result = service.update(accountId, relation.getFamilyMemberId(), request(AllergyStatus.NONE_REPORTED, List.of()));
        assertEquals(child.getProfileId(), result.patientProfileId());
        assertEquals("Nguyễn Minh An", child.getFullName());
        assertEquals("", child.getAllergies());
        verify(profiles).save(child);
    }

    @Test
    void inactiveMemberCannotBeEdited() {
        ownedRelation();
        relation.setIsActive(false);
        assertThrows(BadRequestException.class,
                () -> service.update(accountId, relation.getFamilyMemberId(), request(null, null)));
        verifyNoInteractions(profiles);
    }

    @Test
    void unknownOrForeignRelationIsNotFoundForEveryMutation() {
        ownerExists();
        UUID id = UUID.randomUUID();
        assertThrows(ResourceNotFoundException.class, () -> service.update(accountId, id, request(null, null)));
        assertThrows(ResourceNotFoundException.class, () -> service.archive(accountId, id));
        assertThrows(ResourceNotFoundException.class, () -> service.restore(accountId, id));
        verifyNoInteractions(profiles, appointments, visits);
        verify(members, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource({"true,false", "false,true", "true,true", "false,false"})
    void archiveRequiresNoUpcomingAppointmentOrActiveVisit(boolean appointment, boolean visit) {
        ownedRelation();
        when(appointments.existsByCustomer_ProfileIdAndStatusInAndScheduledAtGreaterThanEqual(
                eq(child.getProfileId()), eq(List.of(AppointmentStatus.PENDING, AppointmentStatus.RESCHEDULED)), any()))
                .thenReturn(appointment);
        when(visits.existsByCustomer_ProfileIdAndStatusIn(child.getProfileId(), List.of(VisitStatus.CHECKED_IN, VisitStatus.IN_PROGRESS)))
                .thenReturn(visit);
        if (appointment || visit) {
            assertThrows(ConflictException.class, () -> service.archive(accountId, relation.getFamilyMemberId()));
            assertTrue(relation.getIsActive());
            verify(members, never()).save(any());
        } else {
            service.archive(accountId, relation.getFamilyMemberId());
            assertFalse(relation.getIsActive());
            verify(members).save(relation);
        }
    }

    @Test
    void archiveIsIdempotentAndRestoreKeepsSameProfile() {
        ownedRelation();
        relation.setIsActive(false);
        service.archive(accountId, relation.getFamilyMemberId());
        verify(members, never()).save(any());
        verifyNoInteractions(appointments, visits);
        when(members.save(relation)).thenReturn(relation);
        var response = service.restore(accountId, relation.getFamilyMemberId());
        assertTrue(response.active());
        assertEquals(child.getProfileId(), response.patientProfileId());
        verifyNoInteractions(profiles);
    }
}
