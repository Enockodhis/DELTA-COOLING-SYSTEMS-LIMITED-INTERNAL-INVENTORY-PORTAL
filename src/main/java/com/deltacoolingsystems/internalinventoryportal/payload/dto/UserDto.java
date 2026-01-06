package com.deltacoolingsystems.internalinventoryportal.payload.dto;

import com.deltacoolingsystems.internalinventoryportal.domain.UserRole;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserDto {
    private Long id;
    private String firstName;      // Make sure this exists
    private String lastName;
    private String fullName;
    private String email;
    private String phone;
    private UserRole role;
    private String password;       // Only for registration, not for responses
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLogin;
}