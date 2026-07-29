package org.example.doansummer2026.dto.medicalRecord;

import org.example.doansummer2026.enums.BloodType;
import org.example.doansummer2026.enums.Gender;
import org.example.doansummer2026.model.Appointment;
import org.example.doansummer2026.model.MedicalRecord;
import org.example.doansummer2026.model.MedicalRecord;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO tra ve cho API lễ tân quản lý hồ sơ bệnh án.
 * Chỉ bao gồm các trường cần thiết cho danh sách và tìm kiếm.
 */
public record ReceptionistRecordResponse(
        UUID id,
        String code,           // Mã bệnh án
        String fullName,       // Họ và tên bệnh nhân
        String phone,          // Số điện thoại
        Integer age,           // Tuổi
        String gender,         // Giới tính (Nam/Nữ)
        String bloodType,      // Nhóm máu
        String lastVisitDate,  // Ngày khám gần nhất
        String lastVisitContent // Nội dung khám (chief complaint)
) {
    public static ReceptionistRecordResponse from(MedicalRecord r) {
        String fullName = null;
        String phone = null;
        Integer age = null;
        String genderStr = null;
        String bloodTypeStr = null;

        // Xử lý cả khách đăng ký và khách vãng lai (guest)
        if (r.getVisit() != null && r.getVisit().getCustomer() != null) {
            // Khách đã đăng ký (có profile)
            fullName = r.getVisit().getCustomer().getFullName();
            phone = r.getVisit().getCustomer().getPhone();

            // Tính tuổi
            if (r.getVisit().getCustomer().getDateOfBirth() != null) {
                age = LocalDate.now().getYear() - r.getVisit().getCustomer().getDateOfBirth().getYear();
            }

            // Giới tính
            if (r.getVisit().getCustomer().getGender() != null) {
                genderStr = switch (r.getVisit().getCustomer().getGender()) {
                    case MALE -> "Nam";
                    case FEMALE -> "Nữ";
                    default -> "Khác";
                };
            }

            // Nhóm máu
            if (r.getVisit().getCustomer().getBloodType() != null) {
                bloodTypeStr = r.getVisit().getCustomer().getBloodType().getDisplay();
            }
        } else if (r.getVisit() != null && r.getVisit().getAppointment() != null
                && Boolean.TRUE.equals(r.getVisit().getAppointment().getIsGuest())) {
            // Khách vãng lai (guest)
            fullName = r.getVisit().getAppointment().getGuestFullName();
            phone = r.getVisit().getAppointment().getGuestPhone();
            age = r.getVisit().getAppointment().getGuestAge();

            if (r.getVisit().getAppointment().getGuestGender() != null) {
                genderStr = switch (r.getVisit().getAppointment().getGuestGender()) {
                    case MALE -> "Nam";
                    case FEMALE -> "Nữ";
                    default -> "Khác";
                };
            }
            // Guest không có bloodType
        }

        // Ngày khám gần nhất
        String lastVisitDateStr = null;
        if (r.getVisit() != null && r.getVisit().getCheckInTime() != null) {
            lastVisitDateStr = r.getVisit().getCheckInTime().toLocalDate().toString();
        }

        // Nội dung khám (chief complaint)
        String lastVisitContentStr = r.getChiefComplaint();

        return new ReceptionistRecordResponse(
                r.getRecordId(),
                r.getRecordCode(),
                fullName,
                phone,
                age,
                genderStr,
                bloodTypeStr,
                lastVisitDateStr,
                lastVisitContentStr
        );
    }
}