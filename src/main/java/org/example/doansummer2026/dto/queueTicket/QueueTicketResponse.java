package org.example.doansummer2026.dto.queueTicket;

import org.example.doansummer2026.enums.QueueStatus;
import org.example.doansummer2026.enums.Gender;
import org.example.doansummer2026.enums.BloodType;
import org.example.doansummer2026.model.QueueTicket;
import org.example.doansummer2026.dto.medicalRecord.MedicalRecordResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record QueueTicketResponse(
        UUID ticketId,
        UUID visitId,
        UUID recordId,         // ID ho so benh an (null neu chua co)
        MedicalRecordResponse medicalRecord, // Thong tin chi tiet medical record (null neu chua co)
        // Thong tin benh nhan (day du tu Profile)
        String patientCode,      // Ma benh nhan (phone)
        String patientName,      // Ten benh nhan
        String patientPhone,     // So dien thoai
        String patientEmail,     // Email
        Gender patientGender,    // Gioi tinh
        LocalDate patientDob,    // Ngay sinh
        BloodType patientBloodType, // Nhom mau
        String lastVisit,      // Ngay check-in gan nhat
        String history,        // Lich su kham (so luong visit)
        // Thong tin khoa dich vu
        UUID departmentId,
        String departmentName,
        // So luong cho phong (de frontend chon phong it hon)
        Integer waitingCount,    // So luong benh nhan dang cho/CALLED
        UUID serviceId,
        String serviceName,
        BigDecimal servicePrice,
        LocalDate workDate,
        Integer queueNumber,
        QueueStatus status,
        LocalDateTime calledAt,
        LocalDateTime completedAt
) {
    public static QueueTicketResponse from(QueueTicket q) {
        return from(q, null, null, null);
    }

    public static QueueTicketResponse from(QueueTicket q, UUID recordId, Integer waitingCount) {
        return from(q, recordId, waitingCount, null);
    }

    public static QueueTicketResponse from(QueueTicket q, UUID recordId, Integer waitingCount, MedicalRecordResponse medicalRecord) {
        UUID visitId = q.getVisit() != null ? q.getVisit().getVisitId() : null;

        // Thong tin benh nhan (day du tu Profile)
        String patientCode = null;
        String patientName = null;
        String patientPhone = null;
        String patientEmail = null;
        Gender patientGender = null;
        LocalDate patientDob = null;
        BloodType patientBloodType = null;
        String lastVisit = null;
        String history = null;
        if (q.getVisit() != null && q.getVisit().getCustomer() != null) {
            var customer = q.getVisit().getCustomer();
            patientCode = customer.getPhone();
            patientName = customer.getFullName();
            patientPhone = customer.getPhone();
            patientEmail = customer.getEmail();
            patientGender = customer.getGender();
            patientDob = customer.getDateOfBirth();
            patientBloodType = customer.getBloodType();
            lastVisit = q.getVisit().getCheckInTime() != null ?
                    q.getVisit().getCheckInTime().toLocalDate().toString() : null;
            // TODO: dem so luong visit - can query them
        }

        UUID deptId = q.getDepartment() != null ? q.getDepartment().getDepartmentId() : null;
        String deptName = q.getDepartment() != null ? q.getDepartment().getName() : null;
        UUID serviceId = q.getService() != null ? q.getService().getServiceId() : null;
        String serviceName = q.getService() != null ? q.getService().getName() : null;
        BigDecimal servicePrice = q.getService() != null ? q.getService().getPrice() : null;
        return new QueueTicketResponse(q.getTicketId(), visitId, recordId, medicalRecord,
                patientCode, patientName, patientPhone, patientEmail,
                patientGender, patientDob, patientBloodType,
                lastVisit, history,
                deptId, deptName, waitingCount,
                serviceId, serviceName, servicePrice,
                q.getWorkDate(), q.getQueueNumber(), q.getStatus(), q.getCalledAt(), q.getCompletedAt());
    }
}



