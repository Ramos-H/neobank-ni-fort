package com.neobank.account_service;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final AccountRepository repository;

    public DataSeeder(AccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() == 0) {
            // Customer 1 (Juan) receives initial account "001"[cite: 1]
            repository.save(new Account(1L, "001", "juan", new BigDecimal("5000.00")));
            
            // Customer 2 (Maria) receives initial account "001"[cite: 1]
            repository.save(new Account(2L, "002", "maria", new BigDecimal("3200.00")));

            repository.save(new Account(2L, "003", "maria", new BigDecimal("3100.00")));

            repository.save(new Account(2L, "004", "maria", new BigDecimal("3100.00")));
        }
    }
}