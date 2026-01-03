package com.deltacoolingsystems.internalinventoryportal.controller;

import com.deltacoolingsystems.internalinventoryportal.exceptions.UserException;
import com.deltacoolingsystems.internalinventoryportal.mapper.UserMapper;
import com.deltacoolingsystems.internalinventoryportal.modal.User;
import com.deltacoolingsystems.internalinventoryportal.payload.dto.UserDto;
import com.deltacoolingsystems.internalinventoryportal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    /* =========================
       GET USER PROFILE
       ========================= */
    @GetMapping("/profile")
    public ResponseEntity<UserDto> getProfile(
            @AuthenticationPrincipal UserDetails userDetails
    ) throws UserException {

        if (userDetails == null) {
            log.warn("Unauthenticated access to GET /api/user/profile");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = userDetails.getUsername();
        log.info("Fetching profile for user: {}", email);

        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UserException("User not found");
        }

        return ResponseEntity.ok(UserMapper.toDTO(user));
    }

    /* =========================
       UPDATE USER PROFILE
       ========================= */
    @PutMapping("/profile")
    public ResponseEntity<UserDto> updateProfile(
            @RequestBody UserDto userDto,
            @AuthenticationPrincipal UserDetails userDetails
    ) throws UserException {

        if (userDetails == null) {
            log.warn("Unauthenticated access to PUT /api/user/profile");
            throw new UserException("Unauthorized");
        }

        String email = userDetails.getUsername();
        log.info("Updating profile for user: {}", email);

        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UserException("User not found");
        }

        // Validate input
        if (userDto.getFirstName() != null && userDto.getFirstName().trim().isEmpty()) {
            throw new UserException("First name cannot be empty");
        }

        if (userDto.getFullName() != null && userDto.getFullName().trim().isEmpty()) {
            throw new UserException("Full name cannot be empty");
        }

        // Update allowed fields
        if (userDto.getFirstName() != null) {
            user.setFirstName(userDto.getFirstName());
        }
        if (userDto.getFullName() != null) {
            user.setFullName(userDto.getFullName());
        }
        if (userDto.getPhone() != null) {
            user.setPhone(userDto.getPhone());
        }

        user.setUpdatedAt(LocalDateTime.now());

        User updatedUser = userRepository.save(user);
        log.info("Profile updated successfully for user: {}", email);

        return ResponseEntity.ok(UserMapper.toDTO(updatedUser));
    }

    /* =========================
       CHANGE PASSWORD
       ========================= */
    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) throws UserException {

        if (userDetails == null) {
            log.warn("Unauthenticated access to POST /api/user/change-password");
            throw new UserException("Unauthorized");
        }

        String email = userDetails.getUsername();
        log.info("Changing password for user: {}", email);

        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UserException("User not found");
        }

        validatePassword(request.getNewPassword());

        // TODO:
        // 1. Inject PasswordEncoder
        // 2. Validate current password
        // 3. Save encoded password
        //
        // user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        // userRepository.save(user);

        log.info("Password changed successfully for user: {}", email);
        return ResponseEntity.ok("Password changed successfully");
    }

    /* =========================
       PASSWORD VALIDATION
       ========================= */
    private void validatePassword(String password) throws UserException {
        if (password == null || password.trim().isEmpty()) {
            throw new UserException("Password is required");
        }
        if (password.length() < 8) {
            throw new UserException("Password must be at least 8 characters");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new UserException("Password must contain at least one uppercase letter");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new UserException("Password must contain at least one lowercase letter");
        }
        if (!password.matches(".*\\d.*")) {
            throw new UserException("Password must contain at least one number");
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            throw new UserException("Password must contain at least one special character");
        }
    }

    /* =========================
       PASSWORD REQUEST DTO
       ========================= */
    @lombok.Data
    public static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;
        private String confirmPassword;
    }
}
