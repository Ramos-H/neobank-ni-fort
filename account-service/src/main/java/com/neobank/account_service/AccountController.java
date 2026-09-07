package com.neobank.account_service;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountRepository repository;

    public AccountController(AccountRepository repository) {
        this.repository = repository;
    }

    // Register a new account with a globally incremented accountId ("001", "002", etc.)
    @PostMapping("/register")
    public ResponseEntity<?> registerAccount(@RequestBody CreateAccountRequest request) {
        // Count total accounts across the entire database to determine the next global ID
        long totalAccounts = repository.count();
        String formattedAccountId = String.format("%03d", totalAccounts + 1);

        BigDecimal startingBalance = request.getInitialBalance() != null 
                ? request.getInitialBalance() 
                : BigDecimal.ZERO;

        Account newAccount = new Account(
                request.getCustomerId(),
                formattedAccountId,
                request.getOwnerUsername(),
                startingBalance
        );

        Account savedAccount = repository.save(newAccount);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedAccount);
    }

    // Look up account balance using the global String accountId ("001", "002", etc.)
    @GetMapping("/{accountId}/balance")
    public ResponseEntity<?> getBalance(@PathVariable String accountId) {
        return repository.findByAccountId(accountId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Service-to-service debit endpoint using String accountId
    @PostMapping("/{accountId}/debit")
    public ResponseEntity<?> debit(@PathVariable String accountId, @RequestBody AdjustBalanceRequest req) {
        return repository.findByAccountId(accountId).map(account -> {
            if (account.getBalance().compareTo(req.getAmount()) < 0) {
                return ResponseEntity.badRequest().body("Insufficient funds");
            }
            account.setBalance(account.getBalance().subtract(req.getAmount()));
            repository.save(account);
            return ResponseEntity.ok(account);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Service-to-service credit endpoint using String accountId
    @PostMapping("/{accountId}/credit")
    public ResponseEntity<?> credit(@PathVariable String accountId, @RequestBody AdjustBalanceRequest req) {
        return repository.findByAccountId(accountId).map(account -> {
            account.setBalance(account.getBalance().add(req.getAmount()));
            repository.save(account);
            return ResponseEntity.ok(account);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}