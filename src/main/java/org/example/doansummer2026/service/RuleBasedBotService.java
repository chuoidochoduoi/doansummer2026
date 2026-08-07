package org.example.doansummer2026.service;

import org.springframework.stereotype.Service;

@Service
public class RuleBasedBotService {

    public String getBotResponse(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return "Xin chào! Mình có thể giúp gì cho bạn? Vui lòng chọn các từ khóa như 'Giờ làm việc', 'Địa chỉ', 'Bảng giá', hoặc 'Gặp lễ tân'.";
        }

        String message = userMessage.toLowerCase();

        if (message.contains("giờ") || message.contains("thời gian")) {
            return "Phòng khám mở cửa từ 7:30 đến 17:00, từ Thứ Hai đến Thứ Bảy hàng tuần. Chủ Nhật nghỉ.";
        }

        if (message.contains("địa chỉ") || message.contains("ở đâu")) {
            return "Phòng khám tọa lạc tại số 123 Đường ABC, Quận XYZ, TP.HCM.";
        }

        if (message.contains("giá") || message.contains("chi phí")) {
            return "Chi phí khám ban đầu thường là 150.000 VNĐ. Tùy thuộc vào chỉ định của bác sĩ mà sẽ có thêm các chi phí dịch vụ xét nghiệm/chụp chiếu khác.";
        }

        if (message.contains("lễ tân") || message.contains("nhân viên") || message.contains("người thật")) {
            return "[HANDOVER]"; // Đặc tả từ khóa để service gọi chuyển giao cho nhân viên
        }

        return "Xin lỗi, mình là hệ thống tự động nên chưa hiểu ý bạn. Bạn có muốn kết nối với Lễ tân không? (Gõ 'Gặp lễ tân' để kết nối).";
    }
}
