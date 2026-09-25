package vn.edu.fpt.cares.dto.queueticket;

import vn.edu.fpt.cares.enums.QueueStatus;

public record QueueTicketUpdateRequest(
        QueueStatus status
) {}




