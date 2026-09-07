package org.example.doansummer2026.config;

import org.example.doansummer2026.enums.DepartmentType;
import org.example.doansummer2026.model.Insurance;
import org.example.doansummer2026.model.InsuranceRule;
import org.example.doansummer2026.repository.InsuranceRepository;
import org.example.doansummer2026.repository.InsuranceRuleRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InsuranceDataSeederTest {
    private final InsuranceRepository insurances = mock(InsuranceRepository.class);
    private final InsuranceRuleRepository rules = mock(InsuranceRuleRepository.class);
    private final InsuranceDataSeeder seeder = new InsuranceDataSeeder(insurances, rules);

    @Test
    void existingInsuranceDataIsNeverChanged() throws Exception {
        when(insurances.count()).thenReturn(1L);
        seeder.run();
        verify(insurances, never()).save(any());
        verifyNoInteractions(rules);
    }

    @Test
    void emptyDatabaseGetsThreeInsurersAndNineConsistentRules() throws Exception {
        when(insurances.count()).thenReturn(0L);
        seeder.run("demo");

        ArgumentCaptor<Insurance> insurer = ArgumentCaptor.forClass(Insurance.class);
        verify(insurances, times(3)).save(insurer.capture());
        assertEquals(List.of("BHYT", "BAOVIET", "PVI"),
                insurer.getAllValues().stream().map(Insurance::getCode).toList());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<InsuranceRule>> batches = ArgumentCaptor.forClass(Iterable.class);
        verify(rules, times(3)).saveAll(batches.capture());
        List<InsuranceRule> all = batches.getAllValues().stream()
                .flatMap(batch -> {
                    java.util.ArrayList<InsuranceRule> values = new java.util.ArrayList<>();
                    batch.forEach(values::add);
                    return values.stream();
                }).toList();
        assertEquals(9, all.size());
        assertTrue(all.stream().allMatch(rule -> rule.getInsurance() != null
                && rule.getDepartmentType() != null && rule.getDiscountPercent() != null));
        assertEquals(3, all.stream().filter(rule -> rule.getDepartmentType() == DepartmentType.EXAMINATION).count());
    }
}
