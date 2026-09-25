package vn.edu.fpt.cares.service;

import lombok.RequiredArgsConstructor;
import vn.edu.fpt.cares.dto.announcement.PublicAnnouncementRequest;
import vn.edu.fpt.cares.dto.announcement.PublicAnnouncementResponse;
import vn.edu.fpt.cares.exception.BadRequestException;
import vn.edu.fpt.cares.exception.ResourceNotFoundException;
import vn.edu.fpt.cares.model.PublicAnnouncement;
import vn.edu.fpt.cares.repository.PublicAnnouncementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PublicAnnouncementService {
    private final PublicAnnouncementRepository repository;

    @Transactional(readOnly = true)
    public List<PublicAnnouncementResponse> listAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(PublicAnnouncementResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<PublicAnnouncementResponse> listVisible() {
        return repository.findVisible(LocalDateTime.now()).stream()
                .map(PublicAnnouncementResponse::from).toList();
    }

    public PublicAnnouncementResponse create(PublicAnnouncementRequest request) {
        validatePeriod(request.startsAt(), request.endsAt());
        PublicAnnouncement value = PublicAnnouncement.builder()
                .title(normalize(request.title()))
                .content(request.content().trim())
                .published(Boolean.TRUE.equals(request.published()))
                .startsAt(request.startsAt())
                .endsAt(request.endsAt())
                .build();
        return PublicAnnouncementResponse.from(repository.save(value));
    }

    public PublicAnnouncementResponse update(UUID id, PublicAnnouncementRequest request) {
        validatePeriod(request.startsAt(), request.endsAt());
        PublicAnnouncement value = find(id);
        value.setTitle(normalize(request.title()));
        value.setContent(request.content().trim());
        value.setPublished(Boolean.TRUE.equals(request.published()));
        value.setStartsAt(request.startsAt());
        value.setEndsAt(request.endsAt());
        return PublicAnnouncementResponse.from(repository.save(value));
    }

    public PublicAnnouncementResponse setPublished(UUID id, boolean published) {
        PublicAnnouncement value = find(id);
        value.setPublished(published);
        return PublicAnnouncementResponse.from(repository.save(value));
    }

    public void delete(UUID id) {
        repository.delete(find(id));
    }

    private PublicAnnouncement find(UUID id) {
        return repository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Thông báo công khai không tồn tại: " + id));
    }

    private void validatePeriod(LocalDateTime startsAt, LocalDateTime endsAt) {
        if (startsAt != null && endsAt != null && !endsAt.isAfter(startsAt)) {
            throw new BadRequestException("Thời gian kết thúc phải sau thời gian bắt đầu");
        }
    }

    private String normalize(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }
}
