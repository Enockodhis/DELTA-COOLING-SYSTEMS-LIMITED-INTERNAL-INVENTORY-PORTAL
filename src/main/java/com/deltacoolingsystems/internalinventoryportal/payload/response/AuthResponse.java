package com.deltacoolingsystems.internalinventoryportal.payload.response;

import com.deltacoolingsystems.internalinventoryportal.payload.dto.UserDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private String jwt;
    private String message;
    private UserDto user;
    private String field; // To indicate which field has error (for error responses)
    private Boolean success; // To indicate if request was successful
    private Integer status; // HTTP status code
    private LocalDateTime timestamp; // Add timestamp for debugging/tracking

    // Factory method for success response with default 200 status
    public static AuthResponse success(String jwt, String message, UserDto user) {
        return AuthResponse.builder()
                .jwt(jwt)
                .message(message)
                .user(user)
                .success(true)
                .status(200)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // Factory method for success response with custom status
    public static AuthResponse success(String jwt, String message, UserDto user, Integer status) {
        return AuthResponse.builder()
                .jwt(jwt)
                .message(message)
                .user(user)
                .success(true)
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // Factory method for error response with field
    public static AuthResponse error(String message, String field, Integer status) {
        return AuthResponse.builder()
                .message(message)
                .field(field)
                .success(false)
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // Factory method for general error (no specific field)
    public static AuthResponse error(String message, Integer status) {
        return AuthResponse.builder()
                .message(message)
                .success(false)
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // Overloaded method for backward compatibility (field without status)
    public static AuthResponse error(String message, String field) {
        return AuthResponse.builder()
                .message(message)
                .field(field)
                .success(false)
                .status(400) // Default to 400 Bad Request
                .timestamp(LocalDateTime.now())
                .build();
    }

    // Overloaded method for backward compatibility (message only)
    public static AuthResponse error(String message) {
        return AuthResponse.builder()
                .message(message)
                .success(false)
                .status(400) // Default to 400 Bad Request
                .timestamp(LocalDateTime.now())
                .build();
    }
}