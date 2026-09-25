package vn.edu.fpt.cares.dto.notification;

import vn.edu.fpt.cares.enums.NotificationStatus;

public record NotificationUpdateRequest(
        NotificationStatus status
) {}




