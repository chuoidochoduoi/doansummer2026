package vn.edu.fpt.cares.service.interfaces;

import vn.edu.fpt.cares.common.PageResponse;
import vn.edu.fpt.cares.dto.queueticket.QueueTicketResponse;
import vn.edu.fpt.cares.dto.queueticket.QueueTicketCreateRequest;
import vn.edu.fpt.cares.dto.queueticket.QueueTicketUpdateRequest;
import vn.edu.fpt.cares.model.QueueTicket;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

/** Service interface for QueueTicket management. */
public interface QueueTicketServiceInterface {
    PageResponse<QueueTicketResponse> search(UUID departmentId, LocalDate workDate,
                                              vn.edu.fpt.cares.enums.QueueStatus status, Pageable pageable);
    QueueTicketResponse get(UUID id);
    QueueTicketResponse create(QueueTicketCreateRequest req);
    QueueTicketResponse update(UUID id, QueueTicketUpdateRequest req);
    QueueTicketResponse call(UUID id);
    QueueTicketResponse startExam(UUID id);
    QueueTicketResponse complete(UUID id);
    QueueTicketResponse skip(UUID id);
    QueueTicketResponse returnToQueue(UUID id);
    void delete(UUID id);
    QueueTicket findById(UUID id);

    /** Lay phieu dang kham (IN_PROGRESS) cua phong - chi co 1 benh nhan/phong. */
    QueueTicketResponse getInprogressByDepartment(UUID departmentId);

    /** Lay tat ca phieu dang kham (IN_PROGRESS) - cho phong kham da khoa. */
    PageResponse<QueueTicketResponse> getAllInprogress(Pageable pageable);
}


