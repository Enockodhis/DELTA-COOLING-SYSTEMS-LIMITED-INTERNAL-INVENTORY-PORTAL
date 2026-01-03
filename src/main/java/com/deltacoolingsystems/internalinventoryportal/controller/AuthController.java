package com.deltacoolingsystems.internalinventoryportal.controller;

import com.deltacoolingsystems.internalinventoryportal.exceptions.UserException;
import com.deltacoolingsystems.internalinventoryportal.payload.dto.UserDto;
import com.deltacoolingsystems.internalinventoryportal.payload.response.AuthResponse;
import com.deltacoolingsystems.internalinventoryportal.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    //  http://localhost:5000/auth/signup
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signupHandler(
            @RequestBody UserDto userDto
    ) throws UserException {
        log.info("Signup request for email: {}", userDto.getEmail());
        return ResponseEntity.ok(authService.signup(userDto));
    }

    //  http://localhost:5000/auth/login
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginHandler(
            @RequestBody UserDto userDto
    ) throws UserException {
        log.info("Login request for email: {}", userDto.getEmail());
        return ResponseEntity.ok(authService.login(userDto));
    }

    //  http://localhost:5000/auth/logout
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logoutHandler() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            String email = authentication.getName();
            log.info("Logout request for user: {}", email);
            SecurityContextHolder.clearContext();
        }

        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    //  http://localhost:5000/auth/refresh
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshTokenHandler(
            @RequestHeader("Authorization") String authorizationHeader
    ) throws UserException {
        log.info("Refresh token request received");

        // Extract token from "Bearer <token>" format
        String oldToken = extractTokenFromHeader(authorizationHeader);

        if (oldToken == null || oldToken.isEmpty()) {
            throw new UserException("Authorization token is required");
        }

        // Call the refresh token service method
        // You'll need to implement this in AuthService
        AuthResponse authResponse = authService.refreshToken(oldToken);

        return ResponseEntity.ok(authResponse);
    }

    //  http://localhost:5000/auth/validate
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateTokenHandler(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        log.info("Token validation request");

        String token = extractTokenFromHeader(authorizationHeader);

        Map<String, Object> response = Map.of(
                "valid", token != null && !token.isEmpty(),
                "message", token != null ? "Token is valid" : "No token provided"
        );

        return ResponseEntity.ok(response);
    }

    //  http://localhost:5000/auth/me
    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> getCurrentUserHandler() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        String email = authentication.getName();
        String authorities = authentication.getAuthorities().toString();

        Map<String, String> userInfo = Map.of(
                "email", email,
                "authorities", authorities,
                "authenticated", "true"
        );

        log.info("Current user info requested for: {}", email);
        return ResponseEntity.ok(userInfo);
    }

    // Helper method to extract token from Authorization header
    private String extractTokenFromHeader(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return authorizationHeader; // Return as-is if no "Bearer " prefix
    }
}