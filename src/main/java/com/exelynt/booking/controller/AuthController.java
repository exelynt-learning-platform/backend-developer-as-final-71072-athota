package com.exelynt.booking.controller;

import com.exelynt.booking.dto.AuthResponse;
import com.exelynt.booking.dto.LoginRequest;
import com.exelynt.booking.dto.RegisterRequest;
import com.exelynt.booking.security.UserPrincipal;
import com.exelynt.booking.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Endpoints for user registration, JWT login authentication, and profile introspection")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and return JWT bearer token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    private final java.util.Map<String, Long> registrationCache = new java.util.concurrent.ConcurrentHashMap<>();

    @PostMapping("/register")
    @Operation(summary = "Register a new standard user account (ROLE_USER only)")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest, jakarta.servlet.http.HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        long now = System.currentTimeMillis();
        if (registrationCache.containsKey(clientIp) && (now - registrationCache.get(clientIp)) < 60000) {
            throw new com.exelynt.booking.exception.BadRequestException("Too many registration attempts. Please wait a minute.");
        }
        registrationCache.put(clientIp, now);
        
        AuthResponse response = authService.register(registerRequest);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/me")
    @Operation(summary = "Get details of currently authenticated user from JWT")
    public ResponseEntity<UserPrincipal> getCurrentUser(@AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(currentUser);
    }
}
