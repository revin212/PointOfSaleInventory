package com.smartpos.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class SmartPosApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartPosApplication.class, args);
    }
}
