package org.example.doansummer2026;

import org.example.doansummer2026.config.SmsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.TimeZone;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(SmsProperties.class)
@EnableScheduling
public class DoAnSummer2026Application {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(DoAnSummer2026Application.class, args);
    }

    @Bean
    public CommandLineRunner fixNationalIdNull(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                jdbcTemplate.execute("ALTER TABLE staff_info ALTER COLUMN national_id DROP NOT NULL");
                System.out.println("Successfully altered staff_info.national_id to be nullable");
            } catch (Exception e) {
                System.out.println("Could not alter staff_info.national_id: " + e.getMessage());
            }
        };
    }
}




