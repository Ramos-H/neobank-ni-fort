package com.neobank.auth_service.dto;

public class LoginResponse {
    private String token;
    private String type = "Bearer";
    private String customerId;
    private String username;

    public LoginResponse(String token, String customerId, String username) {
        this.token = token;
        this.customerId = customerId;
        this.username = username;
    }

    public String getToken() { return token; }
    public String getType() { return type; }
    public String getCustomerId() { return customerId; }
    public String getUsername() { return username; }
}
