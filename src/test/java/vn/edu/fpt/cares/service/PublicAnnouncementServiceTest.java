package vn.edu.fpt.cares.service;

import vn.edu.fpt.cares.dto.announcement.PublicAnnouncementRequest;
import vn.edu.fpt.cares.exception.BadRequestException;
import vn.edu.fpt.cares.exception.ResourceNotFoundException;
import vn.edu.fpt.cares.model.PublicAnnouncement;
import vn.edu.fpt.cares.repository.PublicAnnouncementRepository;
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
    void create_ShouldNormalizeAndPersistAnnouncement() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(new PublicAnnouncementRequest(
                "  Lịch   nghỉ lễ  ", " Nội dung thông báo ", true, null, null
        ));

        ArgumentCaptor<PublicAnnouncement> captor = ArgumentCaptor.forClass(PublicAnnouncement.class);
        verify(repository).save(captor.capture());
        assertEquals("Lịch nghỉ lễ", captor.getValue().getTitle());
        assertEquals("Nội dung thông báo", captor.getValue().getContent());
        assertTrue(captor.getValue().getPublished());
    }

    @Test
    void create_ShouldRejectInvalidDisplayPeriod() {
        LocalDateTime startsAt = LocalDateTime.now().plusDays(2);
        LocalDateTime endsAt = startsAt.minusHours(1);

        assertThrows(BadRequestException.class, () -> service.create(
                new PublicAnnouncementRequest("Thông báo", "Nội dung", true, startsAt, endsAt)
        ));
        verify(repository, never()).save(any());
    }

    @Test
    void create_AllowsOpenEndedDisplayPeriod() {
        LocalDateTime startsAt = LocalDateTime.now().plusDays(1);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(new PublicAnnouncementRequest(
                "Thông báo", "Nội dung", true, startsAt, null));

        assertEquals(startsAt, result.startsAt());
        assertNull(result.endsAt());
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

    @Test
    void listAllUpdatePublishAndDelete_ShouldUseStoredAnnouncement() {
        UUID id = UUID.randomUUID();
        PublicAnnouncement value = PublicAnnouncement.builder().announcementId(id)
                .title("Cũ").content("Nội dung cũ").published(false).build();
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(value));
        when(repository.findById(id)).thenReturn(java.util.Optional.of(value));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(1, service.listAll().size());
        service.update(id, new PublicAnnouncementRequest("  Tiêu   đề mới  ", " Nội dung mới ", null,
                LocalDateTime.now(), LocalDateTime.now().plusHours(1)));
        assertEquals("Tiêu đề mới", value.getTitle());
        assertEquals("Nội dung mới", value.getContent());
        assertFalse(value.getPublished());

        service.setPublished(id, true);
        service.delete(id);
        assertTrue(value.getPublished());
        verify(repository).delete(value);
        verify(repository, times(2)).save(value);
    }

    @Test
    void updatePublishAndDelete_ShouldRejectUnknownAnnouncement() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(java.util.Optional.empty());
        PublicAnnouncementRequest request = new PublicAnnouncementRequest("Tiêu đề", "Nội dung", true, null, null);

        assertThrows(ResourceNotFoundException.class, () -> service.update(id, request));
        assertThrows(ResourceNotFoundException.class, () -> service.setPublished(id, true));
        assertThrows(ResourceNotFoundException.class, () -> service.delete(id));
        verify(repository, never()).save(any());
        verify(repository, never()).delete(any());
    }
}
