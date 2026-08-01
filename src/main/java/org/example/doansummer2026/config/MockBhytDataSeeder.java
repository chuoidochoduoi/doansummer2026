package org.example.doansummer2026.config;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.model.Insurance;
import org.example.doansummer2026.model.MockBhytCard;
import org.example.doansummer2026.repository.InsuranceRepository;
import org.example.doansummer2026.repository.MockBhytCardRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@Order(2)
@RequiredArgsConstructor
public class MockBhytDataSeeder implements CommandLineRunner {

    private final MockBhytCardRepository cardRepository;
    private final InsuranceRepository insuranceRepository;

    @Override
    public void run(String... args) throws Exception {
        if (cardRepository.count() == 0) {
            Insurance bhyt = insuranceRepository.findByCode("BHYT")
                    .orElse(null);
            if (bhyt == null) return;

            Insurance baoViet = insuranceRepository.findByCode("BAOVIET").orElse(bhyt);

            List<MockBhytCard> cards = new ArrayList<>();
            Random random = new Random();

            String[] firstNames = {"Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Huỳnh", "Phan", "Vũ", "Võ", "Đặng", "Bùi", "Đỗ", "Hồ", "Ngô", "Dương", "Lý"};
            String[] middleNames = {"Văn", "Thị", "Hữu", "Minh", "Thanh", "Đức", "Thái", "Xuân", "Hoài", "Quang"};
            String[] lastNames = {"An", "Bình", "Cường", "Dũng", "Em", "Phúc", "Giang", "Hải", "Khánh", "Linh", "My", "Nam", "Oanh", "Phong", "Quân", "Sơn", "Trang", "Uyên", "Vinh", "Vy"};

            for (int i = 1; i <= 50; i++) {
                String prefix = i <= 40 ? "DN40101" : "HS40101";
                String cardNumber = prefix + String.format("%08d", 100000 + i);
                
                String fullName = firstNames[random.nextInt(firstNames.length)] + " " +
                        middleNames[random.nextInt(middleNames.length)] + " " +
                        lastNames[random.nextInt(lastNames.length)];

                LocalDate dob = LocalDate.of(1950 + random.nextInt(60), 1 + random.nextInt(12), 1 + random.nextInt(28));
                LocalDate validFrom = LocalDate.of(2023, 1, 1);
                LocalDate validTo = LocalDate.of(2028, 12, 31);
                
                Insurance ins = (i <= 45) ? bhyt : baoViet;

                MockBhytCard card = MockBhytCard.builder()
                        .cardNumber(cardNumber)
                        .fullName(fullName)
                        .dateOfBirth(dob)
                        .insurance(ins)
                        .validFrom(validFrom)
                        .validTo(validTo)
                        .build();
                cards.add(card);
            }
            
            cardRepository.saveAll(cards);
            System.out.println("Seeded 50 mock BHYT cards. Example codes: DN4010100100001 to DN4010100100040, HS4010100100041 to HS4010100100050");
        }
    }
}
