package com.boxdispatch.Responses;

import lombok.Builder;
import lombok.Data;
 
@Data
@Builder
public class AuthResponse {
    private String token;
    private String tokenType;
    private long expiresIn;
    private String username;
    private String email;

    public AuthResponse() {
    }

    public AuthResponse(String token, String tokenType, long expiresIn, String username, String email) {
        this.token = token;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.username = username;
        this.email = email;
    }

    public String getToken() {
        return this.token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return this.tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresIn() {
        return this.expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

}