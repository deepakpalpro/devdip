package com.banking.forms.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.banking.forms")
@EntityScan(basePackages = "com.banking.forms")
@EnableJpaRepositories(basePackages = "com.banking.forms")
public class BankingFormsApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankingFormsApplication.class, args);
    }
}
