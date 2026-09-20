package org.example.doansummer2026.dto.medicalrecord;

import org.example.doansummer2026.model.MedicalRecord;
import java.time.LocalDateTime;
import java.util.UUID;

public record FeedbackResponse(UUID recordId, String serviceName, String patientName, String doctorName,
 Integer overallRating, String comment,
 String status, String managerResponse, String respondedByName,
 LocalDateTime ratedAt, LocalDateTime respondedAt) {
 public static FeedbackResponse from(MedicalRecord r) {
  return new FeedbackResponse(r.getRecordId(), r.getQueueTicket()!=null&&r.getQueueTicket().getService()!=null?r.getQueueTicket().getService().getName():"Khám bệnh",
   r.getVisit()!=null&&r.getVisit().getCustomer()!=null?r.getVisit().getCustomer().getFullName():null,
   r.getDoctor()!=null&&r.getDoctor().getProfile()!=null?r.getDoctor().getProfile().getFullName():null,
   r.getRatingScore(),r.getRatingComment(),
   r.getFeedbackStatus(),r.getManagerResponse(),
   r.getRespondedBy() != null && r.getRespondedBy().getProfile() != null ? r.getRespondedBy().getProfile().getFullName() : null,
   r.getRatedAt(),r.getRespondedAt());
 }
}
