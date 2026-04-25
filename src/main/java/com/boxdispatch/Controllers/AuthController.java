package com.boxdispatch.Controllers;

import com.boxdispatch.DTO.BoxApiResponse;
import com.boxdispatch.Payloads.LoginRequest;
import com.boxdispatch.Payloads.RegisterRequest;
import com.boxdispatch.Responses.AuthResponse;
import com.boxdispatch.Services.AuthService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@Slf4j
public class AuthController {
 
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** GET /login  →  login.html */
    @GetMapping("/auth/login")
    public String loginPage() {
        return "auth/login";
    }
 
    /** GET /register  →  register.html */
    @GetMapping("/auth/register")
    public String registerPage() {
        return "auth/register";
    }

    /**
     * POST /api/auth/register
     * Creates a new user account and returns a JWT.
     */
    @PostMapping("/api/auth/register")
    @ResponseBody
    public ResponseEntity<BoxApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse auth = authService.register(request);
        return ResponseEntity.ok(BoxApiResponse.success("Registration successful", auth));
    }
 
    /**
     * POST /api/auth/login
     * Authenticates a user and returns a JWT.
     */
    @PostMapping("/api/auth/login")
    @ResponseBody
    public ResponseEntity<BoxApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse auth = authService.login(request);
        return ResponseEntity.ok(BoxApiResponse.success("Login successful", auth));
    }
}
 