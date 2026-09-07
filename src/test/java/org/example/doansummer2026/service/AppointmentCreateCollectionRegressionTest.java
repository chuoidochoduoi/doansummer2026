package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.appointment.AppointmentCreateRequest;
import org.example.doansummer2026.enums.Role;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.model.Account;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentCreateCollectionRegressionTest {
    @Mock AccountRepository accountRepo;
    @Mock ProfileRepository profileRepo;
    @Mock AppointmentRepository repo;
    @Mock ShiftConfigRepository shiftConfigRepository;
    @InjectMocks AppointmentService service;

    static java.util.stream.Stream<Set<UUID>> invalidServiceSelections() {
        return java.util.stream.Stream.of(null, Set.of(), java.util.Collections.singleton(null));
    }

    @ParameterizedTest
    @MethodSource("invalidServiceSelections")
    void invalidSelectionStillUsesBusinessValidation(Set<UUID> serviceIds) {
        UUID accountId = UUID.randomUUID();
        var account = Account.builder().accountId(accountId).role(Role.CUSTOMER).build();
        when(accountRepo.findById(accountId)).thenReturn(Optional.of(account));
        when(profileRepo.findFirstByAccount_AccountId(accountId)).thenReturn(Optional.of(
                Profile.builder().profileId(UUID.randomUUID()).account(account).build()));
        var request = new AppointmentCreateRequest(accountId, LocalDateTime.of(2030, 1, 10, 8, 0),
                null, UUID.randomUUID(), serviceIds);
        assertThrows(BadRequestException.class, () -> service.create(request));
        verifyNoInteractions(repo, shiftConfigRepository);
    }

    @Test
    void realRequestWithSetMustReachShiftValidationWithoutCollectionCastFailure() {
        UUID accountId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();
        var account = Account.builder().accountId(accountId).role(Role.CUSTOMER).build();
        var profile = Profile.builder().profileId(UUID.randomUUID()).account(account).build();
        when(accountRepo.findById(accountId)).thenReturn(Optional.of(account));
        when(profileRepo.findFirstByAccount_AccountId(accountId)).thenReturn(Optional.of(profile));
        // Real DTO, not a mock. Default empty Optional represents a missing shift.
        var request = new AppointmentCreateRequest(accountId, LocalDateTime.of(2030, 1, 10, 8, 0),
                null, shiftId, Set.of(UUID.randomUUID()));
        var error = assertThrows(ResourceNotFoundException.class, () -> service.create(request));
        assertEquals("Ca khám không tồn tại", error.getMessage());
        verify(shiftConfigRepository).findById(shiftId);
        verify(repo, never()).save(any());
    }
}
