package vn.edu.fpt.cares;

import vn.edu.fpt.cares.config.SmsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import java.util.TimeZone;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(SmsProperties.class)
@EnableScheduling
public class CareSApplication {

    public static void main(String[] args) {
        SpringApplication.run(CareSApplication.class, args);
    }
}



