package org.example.doansummer2026;

import org.example.doansummer2026.config.SmsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import java.util.TimeZone;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(SmsProperties.class)
@EnableScheduling
public class DoAnSummer2026Application {

    public static void main(String[] args) {
        SpringApplication.run(DoAnSummer2026Application.class, args);
    }
}



