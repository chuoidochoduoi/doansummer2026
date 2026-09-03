package org.example.doansummer2026.dto.queueticket;

import org.example.doansummer2026.enums.QueueStatus;

public record QueueTicketUpdateRequest(
        QueueStatus status
) {}




