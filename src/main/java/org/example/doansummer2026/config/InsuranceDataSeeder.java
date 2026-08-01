package org.example.doansummer2026.config;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.enums.DepartmentType;
import org.example.doansummer2026.model.Insurance;
import org.example.doansummer2026.model.InsuranceRule;
import org.example.doansummer2026.repository.InsuranceRepository;
import org.example.doansummer2026.repository.InsuranceRuleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class InsuranceDataSeeder implements CommandLineRunner {

    private final InsuranceRepository insuranceRepository;
    private final InsuranceRuleRepository insuranceRuleRepository;

    @Override
    public void run(String... args) throws Exception {
        if (insuranceRepository.count() == 0) {
            // 1. BHYT Nhà Nước
            Insurance bhyt = Insurance.builder()
                    .code("BHYT")
                    .name("Bảo Hiểm Y Tế (Nhà Nước)")
                    .description("Giảm giá 80% cho khám bệnh và xét nghiệm, 50% chẩn đoán hình ảnh")
                    .build();
            insuranceRepository.save(bhyt);

            insuranceRuleRepository.saveAll(List.of(
                    createRule(bhyt, DepartmentType.EXAMINATION, new BigDecimal("80.00")),
                    createRule(bhyt, DepartmentType.LABORATORY, new BigDecimal("80.00")),
                    createRule(bhyt, DepartmentType.IMAGING, new BigDecimal("50.00"))
            ));

            // 2. Bảo hiểm Bảo Việt
            Insurance baoViet = Insurance.builder()
                    .code("BAOVIET")
                    .name("Bảo hiểm Bảo Việt (Cao Cấp)")
                    .description("Giảm giá 100% dịch vụ khám, 30% xét nghiệm")
                    .build();
            insuranceRepository.save(baoViet);

            insuranceRuleRepository.saveAll(List.of(
                    createRule(baoViet, DepartmentType.EXAMINATION, new BigDecimal("100.00")),
                    createRule(baoViet, DepartmentType.LABORATORY, new BigDecimal("30.00")),
                    createRule(baoViet, DepartmentType.IMAGING, new BigDecimal("10.00"))
            ));
            
            // 3. PVI
            Insurance pvi = Insurance.builder()
                    .code("PVI")
                    .name("Bảo hiểm PVI (Cơ Bản)")
                    .description("Giảm giá cố định 5% mọi dịch vụ")
                    .build();
            insuranceRepository.save(pvi);

            insuranceRuleRepository.saveAll(List.of(
                    createRule(pvi, DepartmentType.EXAMINATION, new BigDecimal("5.00")),
                    createRule(pvi, DepartmentType.LABORATORY, new BigDecimal("5.00")),
                    createRule(pvi, DepartmentType.IMAGING, new BigDecimal("5.00"))
            ));

            System.out.println("Seeded real Insurance Data.");
        }
    }

    private InsuranceRule createRule(Insurance insurance, DepartmentType departmentType, BigDecimal discountPercent) {
        return InsuranceRule.builder()
                .insurance(insurance)
                .departmentType(departmentType)
                .discountPercent(discountPercent)
                .build();
    }
}
