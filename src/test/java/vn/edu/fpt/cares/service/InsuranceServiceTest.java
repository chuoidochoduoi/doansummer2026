package vn.edu.fpt.cares.service;

import vn.edu.fpt.cares.enums.DepartmentType;
import vn.edu.fpt.cares.model.Insurance;
import vn.edu.fpt.cares.model.InsuranceRule;
import vn.edu.fpt.cares.repository.InsuranceRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InsuranceServiceTest {

    @Test
    void getAllInsurancesMapsInsuranceAndItsRules() {
        InsuranceRepository repository = mock(InsuranceRepository.class);
        InsuranceService service = new InsuranceService(repository);
        UUID insuranceId = UUID.randomUUID();
        UUID ruleId = UUID.randomUUID();
        Insurance insurance = Insurance.builder()
                .insuranceId(insuranceId)
                .code("BHYT")
                .name("Bảo hiểm y tế")
                .description("Quyền lợi khám chữa bệnh")
                .build();
        insurance.setRules(List.of(InsuranceRule.builder()
                .ruleId(ruleId)
                .insurance(insurance)
                .departmentType(DepartmentType.EXAMINATION)
                .discountPercent(new BigDecimal("80.00"))
                .build()));
        when(repository.findAll()).thenReturn(List.of(insurance));

        var result = service.getAllInsurances();

        assertEquals(1, result.size());
        assertEquals(insuranceId, result.get(0).insuranceId());
        assertEquals("BHYT", result.get(0).code());
        assertEquals(1, result.get(0).rules().size());
        assertEquals(ruleId, result.get(0).rules().get(0).ruleId());
        assertEquals(new BigDecimal("80.00"), result.get(0).rules().get(0).discountPercent());
    }
}
