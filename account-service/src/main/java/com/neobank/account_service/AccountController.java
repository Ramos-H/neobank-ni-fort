package com.neobank.account_service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountRepository repository;

    public AccountController(AccountRepository repository) {
        this.repository = repository;
    }

    // GET /api/accounts/{id}/balance 
    @GetMapping("/{id}/balance")
    public ResponseEntity<?> getBalance(@PathVariable String id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // POST /api/accounts/{id}/debit 
    @PostMapping("/{id}/debit")
    public ResponseEntity<?> debit(@PathVariable String id, @RequestBody AdjustBalanceRequest req) {
        return repository.findById(id).map(account -> {
            if (account.getBalance().compareTo(req.getAmount()) < 0) {
                return ResponseEntity.badRequest().body("Insufficient funds");
            }
            account.setBalance(account.getBalance().subtract(req.getAmount()));
            repository.save(account);
            return ResponseEntity.ok(account);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // POST /api/accounts/{id}/credit
    @PostMapping("/{id}/credit")
    public ResponseEntity<?> credit(@PathVariable String id, @RequestBody AdjustBalanceRequest req) {
        return repository.findById(id).map(account -> {
            account.setBalance(account.getBalance().add(req.getAmount()));
            repository.save(account);
            return ResponseEntity.ok(account);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}