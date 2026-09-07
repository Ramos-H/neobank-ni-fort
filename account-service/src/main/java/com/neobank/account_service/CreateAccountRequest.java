package com.neobank.account_service;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

public class CreateAccountRequest {

    @NotNull
    private Long customerId;

    private String ownerUsername;

    private BigDecimal initialBalance;

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }
    public BigDecimal getInitialBalance() { return initialBalance; }
    public void setInitialBalance(BigDecimal initialBalance) { this.initialBalance = initialBalance; }
}