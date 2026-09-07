package com.neobank.auth_service.dto;

public class RegisterResponse {
    private String message;
    private String customerId;
    private String username;
    private String email;

    public RegisterResponse(String message, String customerId, String username, String email) {
        this.message = message;
        this.customerId = customerId;
        this.username = username;
        this.email = email;
    }

    public String getMessage() { return message; }
    public String getCustomerId() { return customerId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
}
