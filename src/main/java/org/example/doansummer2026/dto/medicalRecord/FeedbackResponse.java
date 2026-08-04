package org.example.doansummer2026.dto.medicalRecord;

import org.example.doansummer2026.model.MedicalRecord;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

public record FeedbackResponse(UUID recordId, String serviceName, String patientName, String doctorName,
 Integer overallRating, Integer doctorRating, Integer waitingRating, Integer staffRating, String comment,
 Boolean contactRequested, String status, String managerResponse, String internalNote,
 String doctorExplanation, LocalDateTime ratedAt, LocalDateTime respondedAt, List<TargetResponse> targets) {
 public record TargetResponse(UUID id, String targetKey, String targetType, String targetName, UUID staffId,
                              UUID sourceRecordId, Integer rating, String comment, String staffExplanation) {}
 public static FeedbackResponse from(MedicalRecord r) {
  return new FeedbackResponse(r.getRecordId(), r.getQueueTicket()!=null&&r.getQueueTicket().getService()!=null?r.getQueueTicket().getService().getName():"Khám bệnh",
   r.getVisit()!=null&&r.getVisit().getCustomer()!=null?r.getVisit().getCustomer().getFullName():null,
   r.getDoctor()!=null&&r.getDoctor().getProfile()!=null?r.getDoctor().getProfile().getFullName():null,
   r.getRatingScore(),r.getDoctorRating(),r.getWaitingRating(),r.getStaffRating(),r.getRatingComment(),r.getContactRequested(),
   r.getFeedbackStatus(),r.getManagerResponse(),r.getInternalNote(),r.getDoctorExplanation(),r.getRatedAt(),r.getRespondedAt(),
   r.getFeedbackTargets().stream().map(t -> new TargetResponse(t.getFeedbackTargetId(), t.getTargetKey(), t.getTargetType(),
           t.getTargetName(), t.getStaff()!=null?t.getStaff().getStaffId():null, t.getSourceRecordId(), t.getRating(),
           t.getComment(), t.getStaffExplanation())).toList());
 }
}
