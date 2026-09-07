package com.neobank.transaction_service;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionRepository repository;
    private final RestTemplate restTemplate;

    // "account-service" here is the Eureka application name, resolved dynamically
    private static final String ACCOUNT_SERVICE_URL = "http://account-service/api/accounts";

    public TransactionController(TransactionRepository repository, RestTemplate restTemplate) {
        this.repository = repository;
        this.restTemplate = restTemplate;
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(@RequestBody TransferRequest request) {
        Transaction transaction = new Transaction(
                request.getFromAccountId(), request.getToAccountId(), request.getAmount(), "PENDING");

        try {
            // 1. Debit the source account
            restTemplate.postForObject(
                    ACCOUNT_SERVICE_URL + "/" + request.getFromAccountId() + "/debit",
                    Map.of("amount", request.getAmount()),
                    Object.class);

            // 2. Credit the destination account
            restTemplate.postForObject(
                    ACCOUNT_SERVICE_URL + "/" + request.getToAccountId() + "/credit",
                    Map.of("amount", request.getAmount()),
                    Object.class);

            transaction.setStatus("SUCCESS");
        } catch (Exception e) {
            transaction.setStatus("FAILED");
            repository.save(transaction);
            return ResponseEntity.badRequest().body("Transfer failed: " + e.getMessage());
        }

        repository.save(transaction);
        return ResponseEntity.ok(transaction);
    }
}