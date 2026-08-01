package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.dto.bhyt.BhytCheckResponse;
import org.example.doansummer2026.model.MockBhytCard;
import org.example.doansummer2026.repository.MockBhytCardRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BhytCheckService {

    private final MockBhytCardRepository mockBhytCardRepository;

    public BhytCheckResponse checkCard(String cardNumber) {
        if (cardNumber == null || cardNumber.isBlank()) {
            return new BhytCheckResponse(cardNumber, null, null, null, null, null, null, false, "Vui lòng nhập mã thẻ BHYT");
        }

        Optional<MockBhytCard> cardOpt = mockBhytCardRepository.findByCardNumberAndDeletedFalse(cardNumber.trim());
        
        if (cardOpt.isEmpty()) {
            return new BhytCheckResponse(cardNumber, null, null, null, null, null, null, false, "Không tìm thấy thông tin thẻ BHYT trên hệ thống");
        }

        MockBhytCard card = cardOpt.get();
        LocalDate today = LocalDate.now();

        if (today.isBefore(card.getValidFrom())) {
            return new BhytCheckResponse(card.getCardNumber(), card.getFullName(), card.getDateOfBirth(), 
                    card.getInsurance().getInsuranceId(), card.getInsurance().getName(), 
                    card.getValidFrom(), card.getValidTo(), false, "Thẻ BHYT chưa có hiệu lực");
        }

        if (today.isAfter(card.getValidTo())) {
            return new BhytCheckResponse(card.getCardNumber(), card.getFullName(), card.getDateOfBirth(), 
                    card.getInsurance().getInsuranceId(), card.getInsurance().getName(), 
                    card.getValidFrom(), card.getValidTo(), false, "Thẻ BHYT đã hết hạn");
        }

        return new BhytCheckResponse(card.getCardNumber(), card.getFullName(), card.getDateOfBirth(), 
                card.getInsurance().getInsuranceId(), card.getInsurance().getName(), 
                card.getValidFrom(), card.getValidTo(), true, "Thẻ BHYT hợp lệ");
    }
}
