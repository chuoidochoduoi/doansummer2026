package org.example.doansummer2026;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.example.doansummer2026.repository.AccountRepository;
import org.example.doansummer2026.repository.ProfileRepository;

@SpringBootTest
public class CheckDBTest {

    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private ProfileRepository profileRepository;

    @Test
    public void printLatestUsers() {
        System.out.println("====== CHECK DB OUTPUT ======");
        System.out.println("Accounts:");
        accountRepository.findAll().forEach(a -> {
            System.out.println(a.getAccountId() + " | " + a.getUsername() + " | " + a.getIsActive());
        });
        
        System.out.println("\nProfiles:");
        profileRepository.findAll().forEach(p -> {
            System.out.println(p.getProfileId() + " | account_id=" + (p.getAccount() != null ? p.getAccount().getAccountId() : "null") + " | phone=" + p.getPhone());
        });
        System.out.println("=============================");
    }
}
