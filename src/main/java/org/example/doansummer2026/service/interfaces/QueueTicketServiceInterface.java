package org.example.doansummer2026.service.interfaces;

import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.queueTicket.QueueTicketResponse;
import org.example.doansummer2026.dto.queueTicket.QueueTicketCreateRequest;
import org.example.doansummer2026.dto.queueTicket.QueueTicketUpdateRequest;
import org.example.doansummer2026.model.QueueTicket;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

/** Service interface for QueueTicket management. */
public interface QueueTicketServiceInterface {
    PageResponse<QueueTicketResponse> search(UUID departmentId, LocalDate workDate,
                                              org.example.doansummer2026.enums.QueueStatus status, Pageable pageable);
    QueueTicketResponse get(UUID id);
    QueueTicketResponse create(QueueTicketCreateRequest req);
    QueueTicketResponse update(UUID id, QueueTicketUpdateRequest req);
    QueueTicketResponse call(UUID id);
    QueueTicketResponse complete(UUID id);
    QueueTicketResponse skip(UUID id);
    void delete(UUID id);
    QueueTicket findById(UUID id);
}