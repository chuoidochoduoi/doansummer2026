package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.queueticket.QueueTicketResponse;
import org.example.doansummer2026.enums.NotificationStatus;
import org.example.doansummer2026.enums.QueueStatus;
import org.example.doansummer2026.enums.SystemRole;
import org.example.doansummer2026.exception.ResourceNotFoundException;
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
}
