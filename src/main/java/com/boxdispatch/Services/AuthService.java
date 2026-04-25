package com.boxdispatch.Services;

import com.boxdispatch.Enums.Roles;
import com.boxdispatch.Exceptions.BadRequestException;
import com.boxdispatch.Exceptions.DuplicateResourceException;
import com.boxdispatch.Interface.IAuthService;
import com.boxdispatch.Interface.IJwtService;
import com.boxdispatch.Models.Users;
import com.boxdispatch.Payloads.LoginRequest;
import com.boxdispatch.Payloads.RegisterRequest;
import com.boxdispatch.Repositories.UsersRepository;
import com.boxdispatch.Responses.AuthResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService implements IAuthService {

    private final UsersRepository       userRepository;
    private final PasswordEncoder       passwordEncoder;
    private final IJwtService           jwtService;
    private final AuthenticationManager authManager;

    public AuthService(UsersRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       IJwtService jwtService,
                       AuthenticationManager authManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authManager = authManager;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent())
            throw new DuplicateResourceException("Email already in use");

        if (userRepository.findByUsername(request.getUsername()).isPresent())
            throw new DuplicateResourceException("Username already taken");

        Users user = Users.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Roles.USER)
                .build();

        userRepository.save(user);

        String token = jwtService.generateToken(user, user.getId());

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new BadRequestException("Invalid username or password.");
        } catch (AuthenticationException e) {
            throw new BadRequestException(e.getMessage());
        }

        Users user = (Users) userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadRequestException("User not found."));

        String token = jwtService.generateToken(user, user.getId());

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }
}