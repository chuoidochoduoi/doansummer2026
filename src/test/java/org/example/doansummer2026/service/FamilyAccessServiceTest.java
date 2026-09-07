package org.example.doansummer2026.service;

import org.example.doansummer2026.enums.FamilyRelationship;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.Account;
import org.example.doansummer2026.model.FamilyMember;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.repository.FamilyMemberRepository;
import org.example.doansummer2026.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FamilyAccessServiceTest {
    @Mock ProfileRepository profiles;
    @Mock FamilyMemberRepository members;
    @InjectMocks FamilyAccessService service;

    private final UUID accountId = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private final Profile owner = Profile.builder()
            .profileId(UUID.fromString("20000000-0000-0000-0000-000000000001")).build();
    private final Profile child = Profile.builder()
            .profileId(UUID.fromString("20000000-0000-0000-0000-000000000002")).build();

    private void ownerExists() {
        when(profiles.findFirstByAccount_AccountId(accountId)).thenReturn(Optional.of(owner));
    }

    private FamilyMember relation(Boolean active) {
        return FamilyMember.builder().ownerProfile(owner).memberProfile(child)
                .relationship(FamilyRelationship.CHILD).isActive(active).build();
    }

    private void memberExists(Boolean active) {
        when(members.findByOwnerProfile_ProfileIdAndMemberProfile_ProfileId(
                owner.getProfileId(), child.getProfileId())).thenReturn(Optional.of(relation(active)));
    }

    @Test
    void missingOwnerIsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> service.ownerProfile(accountId));
        verifyNoInteractions(members);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void selfAccessDoesNotRequireFamilyMembership(boolean explicitProfile) {
        ownerExists();
        UUID target = explicitProfile ? owner.getProfileId() : null;
        assertSame(owner, service.resolveReadableProfile(accountId, target));
        assertSame(owner, service.resolveActiveProfile(accountId, target));
        verifyNoInteractions(members);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = {true, false})
    void existingMemberHistoryRemainsReadableRegardlessOfActiveFlag(Boolean active) {
        ownerExists();
        memberExists(active);
        assertSame(child, service.resolveReadableProfile(accountId, child.getProfileId()));
    }

    @Test
    void unrelatedProfileCannotBeReadOrManaged() {
        ownerExists();
        assertThrows(BadRequestException.class,
                () -> service.resolveReadableProfile(accountId, child.getProfileId()));
        assertThrows(BadRequestException.class,
                () -> service.resolveActiveProfile(accountId, child.getProfileId()));
        verify(members, never()).save(any());
    }

    @Test
    void activeFamilyMemberCanBeManaged() {
        ownerExists();
        memberExists(true);
        assertSame(child, service.resolveActiveProfile(accountId, child.getProfileId()));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = {false})
    void inactiveOrUnspecifiedMembershipCannotBeManaged(Boolean active) {
        ownerExists();
        memberExists(active);
        BadRequestException error = assertThrows(BadRequestException.class,
                () -> service.resolveActiveProfile(accountId, child.getProfileId()));
        assertTrue(error.getMessage().contains("ngừng quản lý"));
        verify(members, never()).save(any());
    }

    @Test
    void selfOnlyListingDoesNotQueryFamily() {
        ownerExists();
        assertEquals(List.of(owner), service.readableProfiles(accountId, false));
        verifyNoInteractions(members);
    }

    @Test
    void familyListingKeepsOwnerFirstAndIncludesHistoricalMembers() {
        ownerExists();
        when(members.findAllByOwnerProfile_ProfileIdOrderByCreatedAtAsc(owner.getProfileId()))
                .thenReturn(List.of(relation(false)));
        assertEquals(List.of(owner, child), service.readableProfiles(accountId, true));
    }

    @Test
    void selfIdentityUsesProfileRatherThanAccountId() {
        ownerExists();
        assertTrue(service.isSelf(accountId, owner.getProfileId()));
        assertFalse(service.isSelf(accountId, child.getProfileId()));
        assertFalse(service.isSelf(accountId, accountId));
    }

    @Test
    void selfRelationshipIsAbsent() {
        ownerExists();
        assertNull(service.relationship(accountId, owner.getProfileId()));
        verifyNoInteractions(members);
    }

    @Test
    void familyRelationshipComesFromMembership() {
        ownerExists();
        memberExists(true);
        assertEquals(FamilyRelationship.CHILD, service.relationship(accountId, child.getProfileId()));
    }

    @Test
    void unrelatedRelationshipIsAbsent() {
        ownerExists();
        assertNull(service.relationship(accountId, child.getProfileId()));
    }

    @Test
    void noPatientHasNoNotificationRecipient() {
        assertNull(service.notificationRecipientProfileId(null));
        verifyNoInteractions(profiles, members);
    }

    @Test
    void AccountHolderReceivesOwnNotifications() {
        owner.setAccount(Account.builder().accountId(accountId).build());
        assertEquals(owner.getProfileId(), service.notificationRecipientProfileId(owner));
        verifyNoInteractions(profiles, members);
    }

    @Test
    void dependentNotificationsGoToOwner() {
        when(members.findByMemberProfile_ProfileId(child.getProfileId()))
                .thenReturn(Optional.of(relation(true)));
        assertEquals(owner.getProfileId(), service.notificationRecipientProfileId(child));
    }

    @Test
    void unlinkedGuestKeepsOwnRecipientId() {
        assertEquals(child.getProfileId(), service.notificationRecipientProfileId(child));
    }
}
