package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.notification.NotificationCreateRequest;
import org.example.doansummer2026.dto.notification.NotificationResponse;
import org.example.doansummer2026.dto.notification.NotificationUpdateRequest;
import org.example.doansummer2026.enums.NotificationChannel;
import org.example.doansummer2026.enums.NotificationStatus;
import org.example.doansummer2026.enums.NotificationType;
import org.example.doansummer2026.enums.SystemRole;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.Account;
import org.example.doansummer2026.model.Notification;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.repository.NotificationRepository;
import org.example.doansummer2026.repository.ProfileRepository;
import org.example.doansummer2026.repository.StaffInfoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository repo;

    @Mock
    private ProfileRepository profileRepo;

    @Mock
    private StaffInfoRepository staffRepo;

    @Mock
    private SimpMessageSendingOperations messagingTemplate;

    @InjectMocks
    private NotificationService notificationService;


    // =========================================================
    // HELPERS
    // =========================================================

    private Account account(UUID id) {
        return Account.builder()
                .accountId(id)
                .username("user01")
                .isActive(true)
                .build();
    }

    private Profile profile(UUID id, Account account) {
        return Profile.builder()
                .profileId(id)
                .fullName("Nguyen Van A")
                .account(account)
                .build();
    }

    private StaffInfo staff(UUID id, Profile profile, SystemRole role) {
        return StaffInfo.builder()
                .staffId(id)
                .staffCode("STF-TEST")
                .profile(profile)
                .systemRole(role)
                .build();
    }

    private Notification notification(
            UUID id,
            Profile recipient,
            NotificationStatus status
    ) {
        return Notification.builder()
                .notificationId(id)
                .recipient(recipient)
                .notificationType(NotificationType.GENERAL)
                .channel(NotificationChannel.IN_APP)
                .title("Thong bao")
                .content("Noi dung")
                .status(status)
                .build();
    }


    // =========================================================
    // SEARCH
    // =========================================================

    @Test
    void search_ShouldReturnMappedPage() {

        UUID recipientId = UUID.randomUUID();

        var pageable =
                PageRequest.of(0, 10);

        Profile recipient =
                profile(
                        recipientId,
                        account(UUID.randomUUID())
                );

        Notification notification =
                notification(
                        UUID.randomUUID(),
                        recipient,
                        NotificationStatus.SENT
                );

        when(
                repo.search(
                        recipientId,
                        NotificationStatus.SENT,
                        pageable
                )
        ).thenReturn(
                new PageImpl<>(List.of(notification))
        );

        var result =
                notificationService.search(
                        recipientId,
                        NotificationStatus.SENT,
                        pageable
                );

        assertNotNull(result);

        verify(repo).search(
                recipientId,
                NotificationStatus.SENT,
                pageable
        );
    }


    // =========================================================
    // FIND / GET
    // =========================================================

    @Test
    void findById_ShouldReturn_WhenFound() {

        UUID id = UUID.randomUUID();

        Notification notification =
                notification(
                        id,
                        profile(
                                UUID.randomUUID(),
                                account(UUID.randomUUID())
                        ),
                        NotificationStatus.SENT
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(notification));

        assertSame(
                notification,
                notificationService.findById(id)
        );
    }


    @Test
    void findById_ShouldThrow_WhenMissing() {

        UUID id = UUID.randomUUID();

        when(repo.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> notificationService.findById(id)
        );
    }


    @Test
    void get_ShouldReturnResponse() {

        UUID id = UUID.randomUUID();

        Notification notification =
                notification(
                        id,
                        profile(
                                UUID.randomUUID(),
                                account(UUID.randomUUID())
                        ),
                        NotificationStatus.SENT
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(notification));

        assertNotNull(
                notificationService.get(id)
        );
    }


    // =========================================================
    // CREATE - RECIPIENT MISSING
    // =========================================================

    @Test
    void create_ShouldThrow_WhenRecipientMissing() {

        UUID recipientId =
                UUID.randomUUID();

        NotificationCreateRequest req =
                new NotificationCreateRequest(
                        recipientId,
                        NotificationType.GENERAL,
                        NotificationChannel.IN_APP,
                        "Thong bao",
                        "Noi dung",
                        "Test",
                        UUID.randomUUID()
                );

        when(profileRepo.findById(recipientId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> notificationService.create(req)
        );

        verify(repo, never())
                .save(any());

        verifyNoInteractions(messagingTemplate);
    }


    // =========================================================
    // CREATE - RECIPIENT WITH ACCOUNT
    // =========================================================

    @Test
    void create_ShouldSaveAndBroadcast_WhenRecipientHasAccount() {

        UUID recipientId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID relatedId = UUID.randomUUID();

        Account account =
                account(accountId);

        Profile recipient =
                profile(
                        recipientId,
                        account
                );

        NotificationCreateRequest req =
                new NotificationCreateRequest(
                        recipientId,
                        NotificationType.GENERAL,
                        NotificationChannel.IN_APP,
                        "Thong bao moi",
                        "Noi dung moi",
                        "QueueTicket",
                        relatedId
                );

        when(profileRepo.findById(recipientId))
                .thenReturn(Optional.of(recipient));

        when(repo.save(any(Notification.class)))
                .thenAnswer(invocation -> {
                    Notification n =
                            invocation.getArgument(0);

                    n.setNotificationId(
                            UUID.randomUUID()
                    );

                    return n;
                });

        var result =
                notificationService.create(req);

        assertNotNull(result);

        ArgumentCaptor<Notification> captor =
                ArgumentCaptor.forClass(
                        Notification.class
                );

        verify(repo)
                .save(captor.capture());

        Notification saved =
                captor.getValue();

        assertSame(
                recipient,
                saved.getRecipient()
        );

        assertEquals(
                NotificationType.GENERAL,
                saved.getNotificationType()
        );

        assertEquals(
                NotificationChannel.IN_APP,
                saved.getChannel()
        );

        assertEquals(
                "Thong bao moi",
                saved.getTitle()
        );

        assertEquals(
                "Noi dung moi",
                saved.getContent()
        );

        assertEquals(
                "QueueTicket",
                saved.getRelatedEntity()
        );

        assertEquals(
                relatedId,
                saved.getRelatedEntityId()
        );

        assertEquals(
                NotificationStatus.SENT,
                saved.getStatus()
        );

        assertNotNull(
                saved.getSentAt()
        );

        verify(messagingTemplate)
                .convertAndSend(
                        eq("/topic/notifications-" + accountId),
                        any(NotificationResponse.class)
                );
    }


    // =========================================================
    // CREATE - RECIPIENT WITHOUT ACCOUNT
    // =========================================================

    @Test
    void create_ShouldNotBroadcast_WhenRecipientHasNoAccount() {

        UUID recipientId =
                UUID.randomUUID();

        Profile recipient =
                profile(
                        recipientId,
                        null
                );

        NotificationCreateRequest req =
                new NotificationCreateRequest(
                        recipientId,
                        NotificationType.GENERAL,
                        NotificationChannel.IN_APP,
                        "Thong bao",
                        "Noi dung",
                        null,
                        null
                );

        when(profileRepo.findById(recipientId))
                .thenReturn(Optional.of(recipient));

        when(repo.save(any(Notification.class)))
                .thenAnswer(invocation -> {
                    Notification n =
                            invocation.getArgument(0);

                    n.setNotificationId(
                            UUID.randomUUID()
                    );

                    return n;
                });

        var result =
                notificationService.create(req);

        assertNotNull(result);

        verifyNoInteractions(
                messagingTemplate
        );
    }


    // =========================================================
    // UPDATE - MISSING
    // =========================================================

    @Test
    void update_ShouldThrow_WhenNotificationMissing() {

        UUID id =
                UUID.randomUUID();

        NotificationUpdateRequest req =
                mock(NotificationUpdateRequest.class);

        when(repo.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> notificationService.update(
                        id,
                        req
                )
        );
    }


    // =========================================================
    // UPDATE STATUS -> SENT + SENTAT NULL
    // =========================================================

    @Test
    void update_ShouldSetSentAt_WhenStatusBecomesSentAndSentAtNull() {

        UUID id =
                UUID.randomUUID();

        Notification n =
                notification(
                        id,
                        profile(
                                UUID.randomUUID(),
                                account(UUID.randomUUID())
                        ),
                        NotificationStatus.PENDING
                );

        n.setSentAt(null);

        NotificationUpdateRequest req =
                mock(NotificationUpdateRequest.class);

        when(req.status())
                .thenReturn(
                        NotificationStatus.SENT
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(n));

        when(repo.save(n))
                .thenReturn(n);

        LocalDateTime before =
                LocalDateTime.now();

        var result =
                notificationService.update(
                        id,
                        req
                );

        LocalDateTime after =
                LocalDateTime.now();

        assertNotNull(result);

        assertEquals(
                NotificationStatus.SENT,
                n.getStatus()
        );

        assertNotNull(
                n.getSentAt()
        );

        assertFalse(
                n.getSentAt()
                        .isBefore(before)
        );

        assertFalse(
                n.getSentAt()
                        .isAfter(after)
        );
    }


    // =========================================================
    // UPDATE STATUS -> SENT + SENTAT EXISTS
    // =========================================================

    @Test
    void update_ShouldKeepExistingSentAt() {

        UUID id =
                UUID.randomUUID();

        Notification n =
                notification(
                        id,
                        profile(
                                UUID.randomUUID(),
                                account(UUID.randomUUID())
                        ),
                        NotificationStatus.PENDING
                );

        LocalDateTime oldSentAt =
                LocalDateTime.now()
                        .minusHours(1);

        n.setSentAt(oldSentAt);

        NotificationUpdateRequest req =
                mock(NotificationUpdateRequest.class);

        when(req.status())
                .thenReturn(
                        NotificationStatus.SENT
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(n));

        when(repo.save(n))
                .thenReturn(n);

        notificationService.update(
                id,
                req
        );

        assertEquals(
                oldSentAt,
                n.getSentAt()
        );
    }


    // =========================================================
    // UPDATE OTHER STATUS
    // =========================================================

    @Test
    void update_ShouldChangeStatusWithoutSettingSentAt_WhenNotSent() {

        UUID id =
                UUID.randomUUID();

        Notification n =
                notification(
                        id,
                        profile(
                                UUID.randomUUID(),
                                null
                        ),
                        NotificationStatus.PENDING
                );

        n.setSentAt(null);

        NotificationUpdateRequest req =
                mock(NotificationUpdateRequest.class);

        when(req.status())
                .thenReturn(
                        NotificationStatus.FAILED
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(n));

        when(repo.save(n))
                .thenReturn(n);

        notificationService.update(
                id,
                req
        );

        assertEquals(
                NotificationStatus.FAILED,
                n.getStatus()
        );

        assertNull(
                n.getSentAt()
        );
    }


    // =========================================================
    // UPDATE FAILURE REASON
    // =========================================================

    @Test
    void update_ShouldUpdateFailureReason() {

        UUID id =
                UUID.randomUUID();

        Notification n =
                notification(
                        id,
                        profile(
                                UUID.randomUUID(),
                                null
                        ),
                        NotificationStatus.PENDING
                );

        NotificationUpdateRequest req =
                mock(NotificationUpdateRequest.class);

        when(req.failureReason())
                .thenReturn(
                        "Send failed"
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(n));

        when(repo.save(n))
                .thenReturn(n);

        notificationService.update(
                id,
                req
        );

        assertEquals(
                "Send failed",
                n.getFailureReason()
        );
    }


    // =========================================================
    // UPDATE EMPTY REQUEST
    // =========================================================

    @Test
    void update_ShouldKeepValues_WhenRequestEmpty() {

        UUID id =
                UUID.randomUUID();

        Notification n =
                notification(
                        id,
                        profile(
                                UUID.randomUUID(),
                                null
                        ),
                        NotificationStatus.PENDING
                );

        n.setFailureReason("Old reason");

        NotificationUpdateRequest req =
                mock(NotificationUpdateRequest.class);

        when(repo.findById(id))
                .thenReturn(Optional.of(n));

        when(repo.save(n))
                .thenReturn(n);

        notificationService.update(
                id,
                req
        );

        assertEquals(
                NotificationStatus.PENDING,
                n.getStatus()
        );

        assertEquals(
                "Old reason",
                n.getFailureReason()
        );
    }


    // =========================================================
    // SEND - ONLY PENDING
    // =========================================================

    @Test
    void send_ShouldReject_WhenStatusIsNotPending() {

        UUID id =
                UUID.randomUUID();

        Notification n =
                notification(
                        id,
                        profile(
                                UUID.randomUUID(),
                                null
                        ),
                        NotificationStatus.SENT
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(n));

        assertThrows(
                BadRequestException.class,
                () -> notificationService.send(id)
        );

        verify(repo, never())
                .save(any());
    }


    @Test
    void send_ShouldSetSentAndSentAt_WhenPending() {

        UUID id =
                UUID.randomUUID();

        Notification n =
                notification(
                        id,
                        profile(
                                UUID.randomUUID(),
                                null
                        ),
                        NotificationStatus.PENDING
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(n));

        when(repo.save(n))
                .thenReturn(n);

        var result =
                notificationService.send(id);

        assertNotNull(result);

        assertEquals(
                NotificationStatus.SENT,
                n.getStatus()
        );

        assertNotNull(
                n.getSentAt()
        );

        verify(repo)
                .save(n);
    }


    // =========================================================
    // MARK READ
    // =========================================================

    @Test
    void markRead_ShouldSetReadAt_WhenReadAtNull() {

        UUID id =
                UUID.randomUUID();

        Notification n =
                notification(
                        id,
                        profile(
                                UUID.randomUUID(),
                                null
                        ),
                        NotificationStatus.SENT
                );

        n.setReadAt(null);

        when(repo.findById(id))
                .thenReturn(Optional.of(n));

        when(repo.save(n))
                .thenReturn(n);

        notificationService.markRead(id);

        assertEquals(
                NotificationStatus.READ,
                n.getStatus()
        );

        assertNotNull(
                n.getReadAt()
        );
    }


    @Test
    void markRead_ShouldKeepExistingReadAt() {

        UUID id =
                UUID.randomUUID();

        Notification n =
                notification(
                        id,
                        profile(
                                UUID.randomUUID(),
                                null
                        ),
                        NotificationStatus.SENT
                );

        LocalDateTime oldReadAt =
                LocalDateTime.now()
                        .minusMinutes(10);

        n.setReadAt(oldReadAt);

        when(repo.findById(id))
                .thenReturn(Optional.of(n));

        when(repo.save(n))
                .thenReturn(n);

        notificationService.markRead(id);

        assertEquals(
                NotificationStatus.READ,
                n.getStatus()
        );

        assertEquals(
                oldReadAt,
                n.getReadAt()
        );
    }


    // =========================================================
    // MARK FAILED
    // =========================================================

    @Test
    void markFailed_ShouldSetStatusAndReason() {

        UUID id =
                UUID.randomUUID();

        Notification n =
                notification(
                        id,
                        profile(
                                UUID.randomUUID(),
                                null
                        ),
                        NotificationStatus.PENDING
                );

        when(repo.findById(id))
                .thenReturn(Optional.of(n));

        when(repo.save(n))
                .thenReturn(n);

        var result =
                notificationService.markFailed(
                        id,
                        "Gateway error"
                );

        assertNotNull(result);

        assertEquals(
                NotificationStatus.FAILED,
                n.getStatus()
        );

        assertEquals(
                "Gateway error",
                n.getFailureReason()
        );
    }


    // =========================================================
    // DELETE
    // =========================================================

    @Test
    void delete_ShouldThrow_WhenMissing() {

        UUID id =
                UUID.randomUUID();

        when(repo.existsById(id))
                .thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> notificationService.delete(id)
        );

        verify(repo, never())
                .deleteById(id);
    }


    @Test
    void delete_ShouldDelete_WhenExists() {

        UUID id =
                UUID.randomUUID();

        when(repo.existsById(id))
                .thenReturn(true);

        notificationService.delete(id);

        verify(repo)
                .deleteById(id);
    }


    // =========================================================
    // UNREAD COUNT
    // =========================================================

    @Test
    void unreadCount_ShouldReturnResponse() {

        UUID recipientId = UUID.randomUUID();

        when(
                repo.countByRecipient_ProfileIdAndStatus(
                        recipientId,
                        NotificationStatus.SENT
                )
        ).thenReturn(7L);

        var result =
                notificationService.unreadCount(recipientId);

        assertNotNull(result);

        verify(repo)
                .countByRecipient_ProfileIdAndStatus(
                        recipientId,
                        NotificationStatus.SENT
                );
    }


    // =========================================================
    // NOTIFY STAFF BY ROLE - EMPTY
    // =========================================================

    @Test
    void notifyStaffByRole_ShouldDoNothing_WhenNoStaff() {

        when(
                staffRepo.findAllBySystemRoleIn(
                        List.of(SystemRole.NURSE)
                )
        ).thenReturn(List.of());

        notificationService.notifyStaffByRole(
                SystemRole.NURSE,
                "Title",
                "Content",
                "Entity",
                UUID.randomUUID()
        );

        verifyNoInteractions(profileRepo);
        verifyNoInteractions(repo);
    }


    // =========================================================
    // NOTIFY STAFF BY ROLE - PROFILE NULL
    // =========================================================

    @Test
    void notifyStaffByRole_ShouldSkipStaff_WhenProfileNull() {

        StaffInfo staff =
                staff(
                        UUID.randomUUID(),
                        null,
                        SystemRole.NURSE
                );

        when(
                staffRepo.findAllBySystemRoleIn(
                        List.of(SystemRole.NURSE)
                )
        ).thenReturn(
                List.of(staff)
        );

        notificationService.notifyStaffByRole(
                SystemRole.NURSE,
                "Title",
                "Content",
                "Entity",
                UUID.randomUUID()
        );

        verifyNoInteractions(profileRepo);
        verifyNoInteractions(repo);
    }


    // =========================================================
    // NOTIFY STAFF BY ROLE - ONE STAFF
    // =========================================================

    @Test
    void notifyStaffByRole_ShouldCreateNotificationForStaffProfile() {

        UUID profileId = UUID.randomUUID();
        UUID relatedId = UUID.randomUUID();

        Profile profile =
                profile(
                        profileId,
                        null
                );

        StaffInfo staff =
                staff(
                        UUID.randomUUID(),
                        profile,
                        SystemRole.NURSE
                );

        when(
                staffRepo.findAllBySystemRoleIn(
                        List.of(SystemRole.NURSE)
                )
        ).thenReturn(
                List.of(staff)
        );

        when(profileRepo.findById(profileId))
                .thenReturn(Optional.of(profile));

        when(repo.save(any(Notification.class)))
                .thenAnswer(invocation -> {
                    Notification n =
                            invocation.getArgument(0);

                    n.setNotificationId(
                            UUID.randomUUID()
                    );

                    return n;
                });

        notificationService.notifyStaffByRole(
                SystemRole.NURSE,
                "Thong bao y ta",
                "Noi dung",
                "QueueTicket",
                relatedId
        );

        verify(repo)
                .save(argThat(n ->
                        n.getRecipient() == profile
                                && n.getNotificationType()
                                == NotificationType.GENERAL
                                && n.getChannel()
                                == NotificationChannel.IN_APP
                                && "Thong bao y ta"
                                .equals(n.getTitle())
                                && "Noi dung"
                                .equals(n.getContent())
                                && "QueueTicket"
                                .equals(n.getRelatedEntity())
                                && relatedId.equals(
                                n.getRelatedEntityId()
                        )
                ));
    }


    // =========================================================
    // NOTIFY STAFF BY ROLE - MULTIPLE
    // =========================================================

    @Test
    void notifyStaffByRole_ShouldCreateForAllStaffWithProfiles() {

        Profile p1 =
                profile(
                        UUID.randomUUID(),
                        null
                );

        Profile p2 =
                profile(
                        UUID.randomUUID(),
                        null
                );

        StaffInfo s1 =
                staff(
                        UUID.randomUUID(),
                        p1,
                        SystemRole.DOCTOR
                );

        StaffInfo s2 =
                staff(
                        UUID.randomUUID(),
                        p2,
                        SystemRole.DOCTOR
                );

        List<StaffInfo> list =
                new ArrayList<>();

        list.add(s1);
        list.add(s2);

        when(
                staffRepo.findAllBySystemRoleIn(
                        List.of(SystemRole.DOCTOR)
                )
        ).thenReturn(list);

        when(profileRepo.findById(p1.getProfileId()))
                .thenReturn(Optional.of(p1));

        when(profileRepo.findById(p2.getProfileId()))
                .thenReturn(Optional.of(p2));

        when(repo.save(any(Notification.class)))
                .thenAnswer(invocation -> {
                    Notification n =
                            invocation.getArgument(0);

                    n.setNotificationId(
                            UUID.randomUUID()
                    );

                    return n;
                });

        notificationService.notifyStaffByRole(
                SystemRole.DOCTOR,
                "Title",
                "Content",
                null,
                null
        );

        verify(repo, times(2))
                .save(any(Notification.class));
    }


    // =========================================================
    // NOTIFY STAFF BY ROLE - EXCEPTION SWALLOWED
    // =========================================================

    @Test
    void notifyStaffByRole_ShouldIgnoreExceptionFromCreate() {

        UUID profileId =
                UUID.randomUUID();

        Profile profile =
                profile(
                        profileId,
                        null
                );

        StaffInfo staff =
                staff(
                        UUID.randomUUID(),
                        profile,
                        SystemRole.NURSE
                );

        when(
                staffRepo.findAllBySystemRoleIn(
                        List.of(SystemRole.NURSE)
                )
        ).thenReturn(
                List.of(staff)
        );

        /*
         * create() sẽ gọi profileRepo.findById().
         * Cho missing để create throw ResourceNotFoundException,
         * notifyStaffByRole() phải catch và bỏ qua.
         */
        when(profileRepo.findById(profileId))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(
                () -> notificationService.notifyStaffByRole(
                        SystemRole.NURSE,
                        "Title",
                        "Content",
                        "Entity",
                        UUID.randomUUID()
                )
        );

        verify(repo, never())
                .save(any());
    }


    // =========================================================
    // NOTIFY STAFF - MIX PROFILE NULL / VALID / ERROR
    // =========================================================

    @Test
    void notifyStaffByRole_ShouldContinue_WhenOneStaffFails() {

        Profile validProfile =
                profile(
                        UUID.randomUUID(),
                        null
                );

        Profile invalidProfile =
                profile(
                        UUID.randomUUID(),
                        null
                );

        StaffInfo withoutProfile =
                staff(
                        UUID.randomUUID(),
                        null,
                        SystemRole.NURSE
                );

        StaffInfo validStaff =
                staff(
                        UUID.randomUUID(),
                        validProfile,
                        SystemRole.NURSE
                );

        StaffInfo failingStaff =
                staff(
                        UUID.randomUUID(),
                        invalidProfile,
                        SystemRole.NURSE
                );

        when(
                staffRepo.findAllBySystemRoleIn(
                        List.of(SystemRole.NURSE)
                )
        ).thenReturn(
                List.of(
                        withoutProfile,
                        failingStaff,
                        validStaff
                )
        );

        when(
                profileRepo.findById(
                        invalidProfile.getProfileId()
                )
        ).thenReturn(Optional.empty());

        when(
                profileRepo.findById(
                        validProfile.getProfileId()
                )
        ).thenReturn(
                Optional.of(validProfile)
        );

        when(repo.save(any(Notification.class)))
                .thenAnswer(invocation -> {
                    Notification n =
                            invocation.getArgument(0);

                    n.setNotificationId(
                            UUID.randomUUID()
                    );

                    return n;
                });

        assertDoesNotThrow(
                () -> notificationService.notifyStaffByRole(
                        SystemRole.NURSE,
                        "Title",
                        "Content",
                        "Entity",
                        UUID.randomUUID()
                )
        );

        /*
         * failingStaff lỗi nhưng vòng for vẫn phải chạy
         * tiếp tới validStaff.
         */
        verify(repo, times(1))
                .save(any(Notification.class));
    }
}