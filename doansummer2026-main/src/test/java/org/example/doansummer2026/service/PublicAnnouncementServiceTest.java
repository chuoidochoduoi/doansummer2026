package org.example.doansummer2026.service;

import org.example.doansummer2026.dto.announcement.PublicAnnouncementRequest;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.model.PublicAnnouncement;
import org.example.doansummer2026.repository.PublicAnnouncementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicAnnouncementServiceTest {
    @Mock
    private PublicAnnouncementRepository repository;

    @InjectMocks
    private PublicAnnouncementService service;

    @Test
    void create_ShouldNormalizeAndStoreActor() {
        UUID actorId = UUID.randomUUID();
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(new PublicAnnouncementRequest(
                "  Lịch   nghỉ lễ  ", " Nội dung thông báo ", true, null, null
        ), actorId);

        ArgumentCaptor<PublicAnnouncement> captor = ArgumentCaptor.forClass(PublicAnnouncement.class);
        verify(repository).save(captor.capture());
        assertEquals("Lịch nghỉ lễ", captor.getValue().getTitle());
        assertEquals("Nội dung thông báo", captor.getValue().getContent());
        assertEquals(actorId, captor.getValue().getCreatedByAccountId());
        assertTrue(captor.getValue().getPublished());
    }

    @Test
    void create_ShouldRejectInvalidDisplayPeriod() {
        LocalDateTime startsAt = LocalDateTime.now().plusDays(2);
        LocalDateTime endsAt = startsAt.minusHours(1);

        assertThrows(BadRequestException.class, () -> service.create(
                new PublicAnnouncementRequest("Thông báo", "Nội dung", true, startsAt, endsAt),
                UUID.randomUUID()
        ));
        verify(repository, never()).save(any());
    }

    @Test
    void listVisible_ShouldUseCurrentPeriodQuery() {
        when(repository.findVisible(any(LocalDateTime.class))).thenReturn(List.of(
                PublicAnnouncement.builder().title("Thông báo").content("Nội dung").published(true).build()
        ));

        var result = service.listVisible();

        assertEquals(1, result.size());
        assertTrue(result.get(0).currentlyVisible());
        verify(repository).findVisible(any(LocalDateTime.class));
    }
}
