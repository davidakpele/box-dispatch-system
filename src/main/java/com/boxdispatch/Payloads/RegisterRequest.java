package com.boxdispatch.Payloads;

import jakarta.validation.constraints.*;
import lombok.Data;
 
@Data
public class RegisterRequest {
 
    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email")
    private String email;
 
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 100, message = "Username must be 3–100 characters")
    private String username;
 
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
}
 