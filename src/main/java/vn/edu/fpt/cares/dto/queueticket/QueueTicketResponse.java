package vn.edu.fpt.cares.dto.queueticket;

import vn.edu.fpt.cares.enums.QueueStatus;
import vn.edu.fpt.cares.enums.Gender;
import vn.edu.fpt.cares.enums.BloodType;
import vn.edu.fpt.cares.model.QueueTicket;
import vn.edu.fpt.cares.dto.medicalrecord.MedicalRecordResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import vn.edu.fpt.cares.model.TestRequest;

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
        vn.edu.fpt.cares.dto.medicalrecord.PatientAllergyResponse patientAllergies,
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
        Integer displayQueueNumber,
        Integer examinationPosition,
        Integer examinationTotalServices,
        QueueStatus status,
        LocalDateTime calledAt,
        LocalDateTime completedAt,
        boolean canCall,
        boolean patientBusy,
        UUID busyDepartmentId,
        String busyDepartmentName,
        QueueStatus busyQueueStatus,
        LocalDateTime busySince,
        List<ServiceProgress> services,
        int totalServices,
        int completedServices,
        Integer waitingPosition,
        String priorityCategory,
        String priorityLabel,
        LocalDateTime appointmentScheduledAt
) {
    public record ServiceProgress(UUID serviceId, String serviceCode, String serviceName, String status) {}

    public static QueueTicketResponse from(QueueTicket q) {
        return from(q, null, null, null);
    }

    public static QueueTicketResponse from(QueueTicket q, UUID recordId, Integer waitingCount) {
        return from(q, recordId, waitingCount, null);
    }

    public static QueueTicketResponse from(QueueTicket q, UUID recordId, Integer waitingCount, MedicalRecordResponse medicalRecord) {
        return from(q, recordId, waitingCount, medicalRecord, false, null);
    }

    public static QueueTicketResponse from(QueueTicket q, UUID recordId, Integer waitingCount,
                                            MedicalRecordResponse medicalRecord, boolean patientBusy,
                                            QueueTicket busyTicket) {
        return from(q, recordId, waitingCount, medicalRecord, patientBusy, busyTicket, List.of());
    }

    public static QueueTicketResponse from(QueueTicket q, UUID recordId, Integer waitingCount,
                                            MedicalRecordResponse medicalRecord, boolean patientBusy,
                                            QueueTicket busyTicket, List<TestRequest> queueTests) {
        return from(q, recordId, waitingCount, medicalRecord, patientBusy, busyTicket, queueTests, null);
    }

    public static QueueTicketResponse from(QueueTicket q, UUID recordId, Integer waitingCount,
                                            MedicalRecordResponse medicalRecord, boolean patientBusy,
                                            QueueTicket busyTicket, List<TestRequest> queueTests,
                                            SameRoomExaminationChainResponse examinationChain) {
        UUID visitId = q.getVisit() != null ? q.getVisit().getVisitId() : null;

        // Thong tin benh nhan (day du tu Profile)
        String patientCode = null;
        String patientName = null;
        String patientPhone = null;
        String patientEmail = null;
        Gender patientGender = null;
        LocalDate patientDob = null;
        BloodType patientBloodType = null;
        vn.edu.fpt.cares.dto.medicalrecord.PatientAllergyResponse patientAllergies =
                vn.edu.fpt.cares.dto.medicalrecord.PatientAllergyResponse.from(null);
        String lastVisit = null;
        String history = null;
        if (q.getVisit() != null && q.getVisit().getCustomer() != null) {
            var customer = q.getVisit().getCustomer();
            patientCode = customer.getPatientCode();
            patientName = customer.getFullName();
            patientPhone = customer.getPhone();
            patientEmail = customer.getEmail();
            patientGender = customer.getGender();
            patientDob = customer.getDateOfBirth();
            patientBloodType = customer.getBloodType();
            patientAllergies = vn.edu.fpt.cares.dto.medicalrecord.PatientAllergyResponse.from(customer);
            lastVisit = q.getVisit().getCheckInTime() != null ?
                    q.getVisit().getCheckInTime().toLocalDate().toString() : null;
            // TODO: dem so luong visit - can query them
        } else if (q.getVisit() != null && q.getVisit().getAppointment() != null) {
            var appointment = q.getVisit().getAppointment();
            patientName = appointment.getGuestFullName();
            patientPhone = appointment.getGuestPhone();
            patientCode = appointment.getAppointmentId() != null
                    ? "BN-TAM-" + appointment.getAppointmentId().toString().replace("-", "").substring(0, 6).toUpperCase()
                    : null;
        }

        UUID deptId = q.getDepartment() != null ? q.getDepartment().getDepartmentId() : null;
        String deptName = q.getDepartment() != null ? q.getDepartment().getName() : null;
        UUID serviceId = q.getService() != null ? q.getService().getServiceId() : null;
        String serviceName = q.getService() != null ? q.getService().getName() : null;
        BigDecimal servicePrice = q.getService() != null ? q.getService().getPrice() : null;
        List<ServiceProgress> serviceProgress = queueTests == null || queueTests.isEmpty()
                ? (q.getService() == null ? List.of() : List.of(new ServiceProgress(
                    q.getService().getServiceId(), q.getService().getServiceCode(),
                    q.getService().getName(), q.getStatus().name())))
                : queueTests.stream()
                .sorted(java.util.Comparator.comparing(TestRequest::getCreatedAt,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .map(test -> new ServiceProgress(
                    test.getService() != null ? test.getService().getServiceId() : null,
                    test.getService() != null ? test.getService().getServiceCode() : null,
                    test.getService() != null ? test.getService().getName() : "Cận lâm sàng",
                    test.getStatus().name())).toList();
        int completedServices = (int) serviceProgress.stream()
                .filter(item -> "COMPLETED".equals(item.status()) || "DONE".equals(item.status())).count();
        return new QueueTicketResponse(q.getTicketId(), visitId, recordId, medicalRecord,
                patientCode, patientName, patientPhone, patientEmail,
                patientGender, patientDob, patientBloodType, patientAllergies,
                lastVisit, history,
                deptId, deptName, waitingCount,
                serviceId, serviceName, servicePrice,
                q.getWorkDate(), q.getQueueNumber(),
                examinationChain != null ? examinationChain.displayQueueNumber() : q.getQueueNumber(),
                examinationChain != null ? examinationChain.currentPosition() : 1,
                examinationChain != null ? examinationChain.totalServices() : 1,
                q.getStatus(), q.getCalledAt(), q.getCompletedAt(),
                !patientBusy && (q.getStatus() == QueueStatus.WAITING || q.getStatus() == QueueStatus.TEST_DONE),
                patientBusy,
                busyTicket != null && busyTicket.getDepartment() != null
                        ? busyTicket.getDepartment().getDepartmentId() : null,
                busyTicket != null && busyTicket.getDepartment() != null
                        ? busyTicket.getDepartment().getName() : null,
                busyTicket != null ? busyTicket.getStatus() : null,
                busyTicket != null ? busyTicket.getCalledAt() : null,
                serviceProgress, serviceProgress.size(), completedServices,
                null, "REGULAR", "Khách trực tiếp", null);
    }

    public QueueTicketResponse withQueuePriority(Integer position, String category, String label,
                                                  LocalDateTime scheduledAt, boolean callable) {
        return new QueueTicketResponse(ticketId, visitId, recordId, medicalRecord,
                patientCode, patientName, patientPhone, patientEmail, patientGender, patientDob,
                patientBloodType, patientAllergies, lastVisit, history, departmentId, departmentName,
                waitingCount, serviceId, serviceName, servicePrice, workDate, queueNumber,
                displayQueueNumber, examinationPosition, examinationTotalServices, status, calledAt,
                completedAt, callable && !patientBusy, patientBusy, busyDepartmentId,
                busyDepartmentName, busyQueueStatus, busySince, services, totalServices,
                completedServices, position, category, label, scheduledAt);
    }
}
