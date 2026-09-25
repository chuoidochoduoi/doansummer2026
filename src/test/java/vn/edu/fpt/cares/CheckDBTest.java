package vn.edu.fpt.cares;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.cares.model.Account;
import vn.edu.fpt.cares.enums.Role;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import vn.edu.fpt.cares.repository.AccountRepository;

@SpringBootTest
@Tag("integration")
@Transactional
public class CheckDBTest extends IsolatedIntegrationTest {

    @Autowired
    private AccountRepository accountRepository;
    
    @Test
    public void persistsAndFindsCustomerInIsolatedDatabase() {
        var saved = accountRepository.saveAndFlush(Account.builder()
                .username("junit_customer").passwordHash("test-only-not-a-login")
                .role(Role.CUSTOMER).isActive(true).build());
        var found = accountRepository.findFirstByUsername("junit_customer").orElseThrow();
        assertNotNull(saved.getAccountId());
        assertEquals(saved.getAccountId(), found.getAccountId());
        assertEquals(Role.CUSTOMER, found.getRole());
        assertTrue(found.getIsActive());
        assertFalse(accountRepository.existsByUsername("junit_missing_customer"));
    }
}
