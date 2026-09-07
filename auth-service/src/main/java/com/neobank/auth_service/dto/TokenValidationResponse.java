package com.neobank.auth_service.dto;

public class TokenValidationResponse {
    private boolean valid;
    private String customerId;
    private String username;

    public TokenValidationResponse(boolean valid, String customerId, String username) {
        this.valid = valid;
        this.customerId = customerId;
        this.username = username;
    }

    public boolean isValid() { return valid; }
    public String getCustomerId() { return customerId; }
    public String getUsername() { return username; }
}
