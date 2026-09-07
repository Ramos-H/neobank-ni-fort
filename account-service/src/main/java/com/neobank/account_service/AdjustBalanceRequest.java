package com.neobank.account_service;

import java.math.BigDecimal;

public class AdjustBalanceRequest {
    private BigDecimal amount;

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}