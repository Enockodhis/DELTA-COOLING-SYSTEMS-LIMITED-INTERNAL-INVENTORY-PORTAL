package com.deltacoolingsystems.internalinventoryportal.controller;

import com.deltacoolingsystems.internalinventoryportal.exceptions.UserException;
import com.deltacoolingsystems.internalinventoryportal.payload.dto.UserDto;
import com.deltacoolingsystems.internalinventoryportal.payload.response.AuthResponse;
import com.deltacoolingsystems.internalinventoryportal.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
            @Valid @RequestBody UserDto userDto
    ) throws UserException {
        log.info("Signup request for email: {}", userDto.getEmail());
        AuthResponse response = authService.signup(userDto);

        // DEBUG LOGGING - Add this to see what's in the response
        log.info("=== DEBUG AUTH RESPONSE ===");
        log.info("Success value: {}", response.getSuccess());
        log.info("Success is null? {}", response.getSuccess() == null);
        log.info("Message: {}", response.getMessage());
        log.info("JWT present? {}", response.getJwt() != null && !response.getJwt().isEmpty());
        log.info("Field value: {}", response.getField());
        log.info("Status value: {}", response.getStatus());
        log.info("User present? {}", response.getUser() != null);
        log.info("===========================");

        if (response.getSuccess() != null && response.getSuccess()) {
            log.info("Returning 200 OK - Registration successful");
            return ResponseEntity.ok(response);
        } else {
            log.warn("Registration unsuccessful - field: {}, message: {}",
                    response.getField(), response.getMessage());

            // Determine appropriate HTTP status based on field
            HttpStatus status = response.getField() != null &&
                    (response.getField().equals("email") ||
                            response.getField().equals("password"))
                    ? HttpStatus.BAD_REQUEST
                    : HttpStatus.CONFLICT;

            log.warn("Returning status: {}", status);
            return ResponseEntity.status(status).body(response);
        }
    }

    //  http://localhost:5000/auth/login
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginHandler(
            @Valid @RequestBody UserDto userDto
    ) throws UserException {
        log.info("Login request for email: {}", userDto.getEmail());

        AuthResponse response = authService.login(userDto);

        if (response.getSuccess() != null && response.getSuccess()) {
            // Success response
            return ResponseEntity.ok(response);
        } else {
            // Error response - determine appropriate status code
            HttpStatus status;

            if (response.getField() != null) {
                // Field-specific errors
                switch (response.getField()) {
                    case "email":
                    case "password":
                        status = HttpStatus.UNAUTHORIZED;
                        break;
                    case "general":
                        status = HttpStatus.FORBIDDEN; // For account disabled, not verified, etc.
                        break;
                    default:
                        status = HttpStatus.BAD_REQUEST;
                }
            } else {
                // General errors
                status = HttpStatus.BAD_REQUEST;
            }

            return ResponseEntity.status(status).body(response);
        }
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
    ) {
        log.info("Refresh token request received");

        // Extract token from "Bearer <token>" format
        String oldToken = extractTokenFromHeader(authorizationHeader);

        if (oldToken == null || oldToken.isEmpty()) {
            AuthResponse errorResponse = AuthResponse.error(
                    "Authorization token is required",
                    "token",
                    HttpStatus.BAD_REQUEST.value()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        try {
            AuthResponse authResponse = authService.refreshToken(oldToken);
            return ResponseEntity.ok(authResponse);
        } catch (UserException e) {
            AuthResponse errorResponse = AuthResponse.error(
                    e.getMessage(),
                    "token",
                    HttpStatus.UNAUTHORIZED.value()
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

    //  http://localhost:5000/auth/validate
    @GetMapping("/validate")
    public ResponseEntity<AuthResponse> validateTokenHandler(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        log.info("Token validation request");

        String token = extractTokenFromHeader(authorizationHeader);

        if (token == null || token.isEmpty()) {
            AuthResponse errorResponse = AuthResponse.error(
                    "No token provided",
                    "token",
                    HttpStatus.BAD_REQUEST.value()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        try {
            // Use the simplified token validation approach
            boolean isValid = validateTokenSimple(token);

            if (isValid) {
                AuthResponse successResponse = AuthResponse.builder()
                        .message("Token is valid")
                        .success(true)
                        .status(HttpStatus.OK.value())
                        .build();
                return ResponseEntity.ok(successResponse);
            } else {
                AuthResponse errorResponse = AuthResponse.error(
                        "Invalid or expired token",
                        "token",
                        HttpStatus.UNAUTHORIZED.value()
                );
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }
        } catch (Exception e) {
            log.error("Token validation error: {}", e.getMessage());
            AuthResponse errorResponse = AuthResponse.error(
                    "Token validation failed",
                    "token",
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    //  http://localhost:5000/auth/me
    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUserHandler() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            AuthResponse errorResponse = AuthResponse.error(
                    "Not authenticated",
                    "auth",
                    HttpStatus.UNAUTHORIZED.value()
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }

        String email = authentication.getName();
        String authorities = authentication.getAuthorities().toString();

        // Create a simple user DTO with basic info
        UserDto userDto = new UserDto();
        userDto.setEmail(email);

        // Try to get more user details if available
        Object principal = authentication.getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            org.springframework.security.core.userdetails.UserDetails userDetails =
                    (org.springframework.security.core.userdetails.UserDetails) principal;
            userDto.setFullName(userDetails.getUsername()); // or appropriate field
        }

        AuthResponse successResponse = AuthResponse.builder()
                .message("User information retrieved successfully")
                .user(userDto)
                .success(true)
                .status(HttpStatus.OK.value())
                .build();

        log.info("Current user info requested for: {}", email);
        return ResponseEntity.ok(successResponse);
    }

    // Helper method to extract token from Authorization header
    private String extractTokenFromHeader(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return authorizationHeader; // Return as-is if no "Bearer " prefix
    }

    // Simplified token validation method
    private boolean validateTokenSimple(String token) {
        // Basic validation - in production, use JWT library
        // This checks if token is not null, not empty, and has minimum length
        // JWT tokens are typically much longer than 10 characters
        return token != null && !token.isEmpty() && token.length() > 10;
    }

    // ========== EXCEPTION HANDLERS ==========
    // ORDER MATTERS: Most specific first, most general last

    // 1. FIRST: Handler for UserException (most specific)
    @ExceptionHandler(UserException.class)
    public ResponseEntity<AuthResponse> handleUserException(UserException ex) {
        log.info("UserException caught in AuthController: {}", ex.getMessage());

        // Determine field based on error message content
        String field = "general";
        String message = ex.getMessage();

        // Analyze the message to determine the field
        String lowerMessage = message.toLowerCase();
        if (lowerMessage.contains("email") ||
                lowerMessage.contains("account") ||
                lowerMessage.contains("registered") ||
                lowerMessage.contains("found")) {
            field = "email";
        } else if (lowerMessage.contains("password") ||
                lowerMessage.contains("incorrect") ||
                lowerMessage.contains("wrong")) {
            field = "password";
        } else if (lowerMessage.contains("first name") || lowerMessage.contains("firstname")) {
            field = "firstName";
        } else if (lowerMessage.contains("last name") || lowerMessage.contains("lastname")) {
            field = "lastName";
        } else if (lowerMessage.contains("token")) {
            field = "token";
        }

        log.info("UserException mapped to field: '{}', message: '{}'", field, message);

        AuthResponse errorResponse = AuthResponse.error(
                message,
                field,
                HttpStatus.UNAUTHORIZED.value()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    // 2. SECOND: Handler for validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AuthResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.info("Validation exception caught: {}", ex.getMessage());

        BindingResult result = ex.getBindingResult();
        FieldError fieldError = result.getFieldError();

        String field = fieldError != null ? fieldError.getField() : "general";
        String message = fieldError != null ? fieldError.getDefaultMessage() : "Validation error";

        AuthResponse errorResponse = AuthResponse.error(
                message,
                field,
                HttpStatus.BAD_REQUEST.value()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    // 3. THIRD: Handler for all other exceptions (most general - should be LAST)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<AuthResponse> handleAllExceptions(Exception ex) {
        log.error("=== UNEXPECTED EXCEPTION CAUGHT ===");
        log.error("Exception type: {}", ex.getClass().getName());
        log.error("Exception message: {}", ex.getMessage());
        log.error("Stack trace:", ex);
        log.error("================================");

        // Check if this might actually be a UserException wrapped in something else
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof UserException) {
                log.info("Found UserException in exception chain, delegating to UserException handler");
                return handleUserException((UserException) cause);
            }
            cause = cause.getCause();
        }

        // Only return generic message for truly unexpected errors
        String userMessage;
        if (ex.getMessage() != null &&
                (ex.getMessage().contains("UserException") ||
                        ex.getMessage().contains("authentication") ||
                        ex.getMessage().contains("password") ||
                        ex.getMessage().contains("email") ||
                        ex.getMessage().contains("account"))) {
            // This might be an authentication error in disguise
            userMessage = ex.getMessage();
            // Try to clean up the message
            if (userMessage.contains("UserException:")) {
                userMessage = userMessage.substring(userMessage.indexOf("UserException:") + 14).trim();
            }
        } else {
            userMessage = "An unexpected error occurred. Please try again later.";
        }

        AuthResponse errorResponse = AuthResponse.error(
                userMessage,
                "general",
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}