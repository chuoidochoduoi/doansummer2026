package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.queueticket.QueueTicketResponse;
import org.example.doansummer2026.enums.NotificationStatus;
import org.example.doansummer2026.enums.QueueStatus;
import org.example.doansummer2026.enums.SystemRole;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.model.CustomerVisit;
import org.example.doansummer2026.model.Department;
import org.example.doansummer2026.model.Notification;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.model.QueueTicket;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.repository.CustomerVisitRepository;
import org.example.doansummer2026.repository.NotificationRepository;
import org.example.doansummer2026.repository.QueueTicketRepository;
import org.example.doansummer2026.repository.StaffInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class QueueReturnRequestServiceTest {

    @Mock NotificationRepository notifications;
    @Mock QueueTicketRepository queues;
    @Mock CustomerVisitRepository visits;
    @Mock StaffInfoRepository staff;
    @Mock FamilyAccessService familyAccess;
    @Mock QueueTicketService queueService;
    @Mock SimpMessagingTemplate messaging;

    QueueReturnRequestService service;
    UUID accountId;
    Profile patient;
    CustomerVisit visit;
    QueueTicket ticket;

    @BeforeEach
    void setup() {
        service = new QueueReturnRequestService(notifications, queues, visits, staff,
                familyAccess, queueService, messaging);
        accountId = UUID.randomUUID();
        patient = Profile.builder().profileId(UUID.randomUUID()).fullName("Nguyễn Văn A").build();
        visit = CustomerVisit.builder().visitId(UUID.randomUUID()).customer(patient).build();
        ticket = QueueTicket.builder().ticketId(UUID.randomUUID()).visit(visit)
                .department(Department.builder().departmentId(UUID.randomUUID()).name("Phòng Nội")
                        .roomCode("INT-101").build())
                .queueNumber(12).status(QueueStatus.SKIPPED)
                .workDate(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"))).build();
        ticket.setCalledAt(LocalDateTime.now().minusMinutes(10));
        ticket.setCreatedAt(LocalDateTime.now().minusMinutes(30));
    }

    @Test
    void customerAndFamilyOwnershipCreateOnePendingRequest() {
        Profile receptionist = Profile.builder().profileId(UUID.randomUUID()).fullName("Lễ tân").build();
        Notification existing = Notification.builder().recipient(receptionist)
                .relatedEntity(QueueReturnRequestService.RELATED_ENTITY).relatedEntityId(ticket.getTicketId())
                .status(NotificationStatus.PENDING).sentAt(LocalDateTime.now()).build();
        when(visits.findById(visit.getVisitId())).thenReturn(Optional.of(visit));
        when(familyAccess.resolveActiveProfile(accountId, patient.getProfileId())).thenReturn(patient);
        when(queues.findAllByVisit_VisitId(visit.getVisitId())).thenReturn(List.of(ticket));
        when(notifications.findAllByRelatedEntityAndRelatedEntityIdAndStatusOrderByCreatedAtAsc(
                QueueReturnRequestService.RELATED_ENTITY, ticket.getTicketId(), NotificationStatus.PENDING))
                .thenReturn(List.of(), List.of(existing));
        when(staff.findAllBySystemRoleIn(anyList())).thenReturn(List.of(StaffInfo.builder()
                .staffId(UUID.randomUUID()).systemRole(SystemRole.RECEPTIONIST).profile(receptionist).build()));

        var created = service.requestForCustomer(accountId, visit.getVisitId());
        var repeated = service.requestForCustomer(accountId, visit.getVisitId());

        assertEquals("PENDING", created.status());
        assertEquals(ticket.getTicketId(), repeated.queueTicketId());
        verify(notifications).saveAll(anyList());
    }

    @Test
    void guestWithWrongLookupCannotRevealVisit() {
        when(visits.findAllByCustomer_PhoneAndCustomer_AccountIsNullOrderByCheckInTimeDesc("0900000000"))
                .thenReturn(List.of());
        assertThrows(ResourceNotFoundException.class,
                () -> service.requestForGuest("VIS-12345678", "0900000000"));
        verify(notifications, never()).saveAll(anyList());
    }

    @Test
    void receptionistConfirmationRestoresTicketAndClosesEveryDuplicateNotification() {
        Notification first = Notification.builder().notificationId(UUID.randomUUID())
                .relatedEntity(QueueReturnRequestService.RELATED_ENTITY).relatedEntityId(ticket.getTicketId())
                .status(NotificationStatus.PENDING).sentAt(LocalDateTime.now().minusMinutes(2)).build();
        Notification second = Notification.builder().notificationId(UUID.randomUUID())
                .relatedEntity(QueueReturnRequestService.RELATED_ENTITY).relatedEntityId(ticket.getTicketId())
                .status(NotificationStatus.PENDING).sentAt(first.getSentAt()).build();
        when(notifications.findAllByRelatedEntityAndRelatedEntityIdAndStatusOrderByCreatedAtAsc(
                QueueReturnRequestService.RELATED_ENTITY, ticket.getTicketId(), NotificationStatus.PENDING))
                .thenReturn(List.of(first, second));
        QueueTicketResponse restored = org.mockito.Mockito.mock(QueueTicketResponse.class);
        when(restored.departmentName()).thenReturn("Phòng Nội");
        when(queueService.confirmReturnToQueue(ticket.getTicketId())).thenReturn(restored);
        when(queues.findById(ticket.getTicketId())).thenReturn(Optional.of(ticket));

        var result = service.confirm(ticket.getTicketId());

        assertEquals("CONFIRMED", result.status());
        assertEquals(NotificationStatus.READ, first.getStatus());
        assertEquals(NotificationStatus.READ, second.getStatus());
        verify(queueService).confirmReturnToQueue(ticket.getTicketId());
        verify(notifications).saveAll(List.of(first, second));
    }

    @Test
    void customerRequestHidesMissingUnownedAndProfilelessVisits() {
        UUID missing = UUID.randomUUID();
        when(visits.findById(missing)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.requestForCustomer(accountId, missing));

        CustomerVisit profileless = CustomerVisit.builder().visitId(UUID.randomUUID()).build();
        when(visits.findById(profileless.getVisitId())).thenReturn(Optional.of(profileless));
        assertThrows(ResourceNotFoundException.class,
                () -> service.requestForCustomer(accountId, profileless.getVisitId()));

        Profile other = Profile.builder().profileId(UUID.randomUUID()).build();
        when(visits.findById(visit.getVisitId())).thenReturn(Optional.of(visit));
        when(familyAccess.resolveActiveProfile(accountId, patient.getProfileId())).thenReturn(other);
        assertThrows(ResourceNotFoundException.class,
                () -> service.requestForCustomer(accountId, visit.getVisitId()));
    }

    @Test
    void guestValidLookupCanCreateRequestAndDeduplicatesRecipients() {
        String code = "VIS-" + visit.getVisitId().toString().substring(0, 8).toUpperCase();
        patient.setPhone("0900000000");
        Profile receptionist = Profile.builder().profileId(UUID.randomUUID()).build();
        when(visits.findAllByCustomer_PhoneAndCustomer_AccountIsNullOrderByCheckInTimeDesc("0900000000"))
                .thenReturn(List.of(visit));
        when(queues.findAllByVisit_VisitId(visit.getVisitId())).thenReturn(List.of(ticket));
        when(notifications.findAllByRelatedEntityAndRelatedEntityIdAndStatusOrderByCreatedAtAsc(
                QueueReturnRequestService.RELATED_ENTITY, ticket.getTicketId(), NotificationStatus.PENDING))
                .thenReturn(List.of());
        when(staff.findAllBySystemRoleIn(anyList())).thenReturn(List.of(
                StaffInfo.builder().profile(receptionist).build(),
                StaffInfo.builder().profile(receptionist).build(),
                StaffInfo.builder().profile(null).build()));

        var response = service.requestForGuest("  " + code.toLowerCase() + " ", "0900 000 000");

        assertEquals("PENDING", response.status());
        verify(notifications).saveAll(org.mockito.ArgumentMatchers.argThat(items ->
                ((List<?>) items).size() == 1));
    }

    @Test
    void invalidGuestInputAndNoReceptionistAreRejected() {
        assertThrows(ResourceNotFoundException.class, () -> service.requestForGuest(null, null));
        assertThrows(ResourceNotFoundException.class, () -> service.requestForGuest("bad", "0900"));

        when(visits.findById(visit.getVisitId())).thenReturn(Optional.of(visit));
        when(familyAccess.resolveActiveProfile(accountId, patient.getProfileId())).thenReturn(patient);
        when(queues.findAllByVisit_VisitId(visit.getVisitId())).thenReturn(List.of(ticket));
        when(notifications.findAllByRelatedEntityAndRelatedEntityIdAndStatusOrderByCreatedAtAsc(
                QueueReturnRequestService.RELATED_ENTITY, ticket.getTicketId(), NotificationStatus.PENDING))
                .thenReturn(List.of());
        when(staff.findAllBySystemRoleIn(anyList())).thenReturn(List.of());
        assertThrows(BadRequestException.class,
                () -> service.requestForCustomer(accountId, visit.getVisitId()));
    }

    @Test
    void onlyValidTodaySkippedRequestsAreListedInRequestOrder() {
        Notification later = Notification.builder().relatedEntityId(ticket.getTicketId())
                .sentAt(LocalDateTime.now().minusMinutes(1)).status(NotificationStatus.PENDING).build();
        Notification earlier = Notification.builder().relatedEntityId(ticket.getTicketId())
                .sentAt(LocalDateTime.now().minusMinutes(3)).status(NotificationStatus.PENDING).build();
        Notification noTicket = Notification.builder().relatedEntityId(null).status(NotificationStatus.PENDING).build();
        when(notifications.findAllByRelatedEntityAndStatusOrderByCreatedAtAsc(
                QueueReturnRequestService.RELATED_ENTITY, NotificationStatus.PENDING))
                .thenReturn(List.of(noTicket, later, earlier));
        when(queues.findById(ticket.getTicketId())).thenReturn(Optional.of(ticket));

        var result = service.pendingRequests();

        assertEquals(1, result.size());
        assertEquals(earlier.getSentAt(), result.get(0).requestedAt());
    }

    @Test
    void receptionistCanListTodaySkippedTicketsWithoutCustomerRequest() {
        when(queues.search(org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"))),
                org.mockito.ArgumentMatchers.eq(QueueStatus.SKIPPED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ticket)));

        var result = service.skippedToday();

        assertEquals(1, result.size());
        assertEquals(ticket.getTicketId(), result.get(0).queueTicketId());
        assertEquals("SKIPPED", result.get(0).status());
    }

    @Test
    void receptionistCanRestoreSkippedTicketWithoutCustomerRequest() {
        when(notifications.findAllByRelatedEntityAndRelatedEntityIdAndStatusOrderByCreatedAtAsc(
                QueueReturnRequestService.RELATED_ENTITY, ticket.getTicketId(), NotificationStatus.PENDING))
                .thenReturn(List.of());
        QueueTicketResponse restored = org.mockito.Mockito.mock(QueueTicketResponse.class);
        when(restored.departmentName()).thenReturn("Phòng Nội");
        when(queueService.confirmReturnToQueue(ticket.getTicketId())).thenReturn(restored);
        when(queues.findById(ticket.getTicketId())).thenReturn(Optional.of(ticket));

        var result = service.restore(ticket.getTicketId());

        assertEquals("CONFIRMED", result.status());
        verify(queueService).confirmReturnToQueue(ticket.getTicketId());
        verify(notifications, never()).saveAll(anyList());
        verify(messaging).convertAndSend("/topic/queue-return-requests", "RETURN_REQUESTS_UPDATED");
    }

    @Test
    void confirmRejectsMissingRequestAndMissingRestoredTicket() {
        when(notifications.findAllByRelatedEntityAndRelatedEntityIdAndStatusOrderByCreatedAtAsc(
                QueueReturnRequestService.RELATED_ENTITY, ticket.getTicketId(), NotificationStatus.PENDING))
                .thenReturn(List.of());
        assertThrows(ConflictException.class, () -> service.confirm(ticket.getTicketId()));

        Notification request = Notification.builder().sentAt(LocalDateTime.now()).build();
        when(notifications.findAllByRelatedEntityAndRelatedEntityIdAndStatusOrderByCreatedAtAsc(
                QueueReturnRequestService.RELATED_ENTITY, ticket.getTicketId(), NotificationStatus.PENDING))
                .thenReturn(List.of(request));
        QueueTicketResponse restored = org.mockito.Mockito.mock(QueueTicketResponse.class);
        when(queueService.confirmReturnToQueue(ticket.getTicketId())).thenReturn(restored);
        when(queues.findById(ticket.getTicketId())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.confirm(ticket.getTicketId()));
    }

    @Test
    void expirationClosesOnlyInvalidRequestsAndRealtimeFailureDoesNotRollback() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        Notification missingId = Notification.builder().notificationId(UUID.randomUUID()).build();
        Notification old = Notification.builder().notificationId(UUID.randomUUID())
                .relatedEntityId(UUID.randomUUID()).build();
        Notification active = Notification.builder().notificationId(UUID.randomUUID())
                .relatedEntityId(ticket.getTicketId()).build();
        QueueTicket oldTicket = QueueTicket.builder().workDate(today.minusDays(1))
                .status(QueueStatus.SKIPPED).build();
        when(notifications.findAllByRelatedEntityAndStatusOrderByCreatedAtAsc(
                QueueReturnRequestService.RELATED_ENTITY, NotificationStatus.PENDING))
                .thenReturn(List.of(missingId, old, active));
        when(queues.findById(old.getRelatedEntityId())).thenReturn(Optional.of(oldTicket));
        when(queues.findById(ticket.getTicketId())).thenReturn(Optional.of(ticket));
        doThrow(new RuntimeException("socket down")).when(messaging)
                .convertAndSend("/topic/queue-return-requests", "RETURN_REQUESTS_UPDATED");

        assertEquals(2, service.expireBefore(today));
        assertEquals(NotificationStatus.FAILED, missingId.getStatus());
        assertEquals(NotificationStatus.FAILED, old.getStatus());
        assertEquals("Yêu cầu quay lại đã hết hiệu lực", old.getFailureReason());
        assertEquals(NotificationStatus.PENDING, active.getStatus());
    }
}
