package com.deltacoolingsystems.internalinventoryportal.service.impl;

import com.deltacoolingsystems.internalinventoryportal.configuration.JwtProvider;
import com.deltacoolingsystems.internalinventoryportal.domain.UserRole;
import com.deltacoolingsystems.internalinventoryportal.exceptions.UserException;
import com.deltacoolingsystems.internalinventoryportal.mapper.UserMapper;
import com.deltacoolingsystems.internalinventoryportal.modal.User;
import com.deltacoolingsystems.internalinventoryportal.payload.dto.UserDto;
import com.deltacoolingsystems.internalinventoryportal.payload.response.AuthResponse;
import com.deltacoolingsystems.internalinventoryportal.repository.UserRepository;
import com.deltacoolingsystems.internalinventoryportal.service.AuthService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final CustomUserImplementation customUserImplementation;

    // Simple in-memory token blacklist (use Redis in production)
    private final ConcurrentHashMap<String, Date> tokenBlacklist = new ConcurrentHashMap<>();

    @Override
    public AuthResponse signup(UserDto userDto) throws UserException {
        log.info("Attempting signup for email: {}", userDto.getEmail());

        // Check if user already exists
        User existingUser = userRepository.findByEmail(userDto.getEmail());
        if (existingUser != null) {
            log.warn("Signup failed - email already registered: {}", userDto.getEmail());
            throw new UserException("Email already registered!");
        }

        // Validate required fields
        validateSignupRequest(userDto);

        // FIXED: Prevent ADMIN registration (not USER registration)
        if (userDto.getRole() != null && userDto.getRole().equals(UserRole.ADMIN)) {
            log.warn("Admin registration attempt blocked for: {}", userDto.getEmail());
            throw new UserException("Admin role registration is not allowed!");
        }

        // Create new user
        User newUser = createUserFromDto(userDto);

        log.info("Creating new user: firstName={}, email={}",
                newUser.getFirstName(), newUser.getEmail());

        User savedUser = userRepository.save(newUser);
        log.info("User created successfully with ID: {}", savedUser.getId());

        // Authenticate the newly created user
        UserDetails userDetails = customUserImplementation.loadUserByUsername(savedUser.getEmail());
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtProvider.generateToken(authentication);

        AuthResponse authResponse = new AuthResponse();
        authResponse.setJwt(jwt);
        authResponse.setMessage("Registered Successfully");

        // Use the updated UserMapper that includes firstName and fullName
        authResponse.setUser(UserMapper.toDTO(savedUser));

        log.info("Signup completed successfully for: {}", savedUser.getEmail());
        return authResponse;
    }

    @Override
    public AuthResponse login(UserDto userDto) throws UserException {
        log.info("Login attempt for email: {}", userDto.getEmail());

        String email = userDto.getEmail();
        String password = userDto.getPassword();

        // Validate login request
        if (email == null || email.trim().isEmpty()) {
            throw new UserException("Email is required");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new UserException("Password is required");
        }

        Authentication authentication = authenticate(email, password);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        String role = authorities.isEmpty() ? "" : authorities.iterator().next().getAuthority();

        String jwt = jwtProvider.generateToken(authentication);

        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UserException("User not found");
        }

        user.setLastLogin(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now()); // Update this timestamp too
        userRepository.save(user);

        AuthResponse authResponse = new AuthResponse();
        authResponse.setJwt(jwt);
        authResponse.setMessage("Login Successful");

        // Use the updated UserMapper
        authResponse.setUser(UserMapper.toDTO(user));

        log.info("Login successful for: {}", email);
        return authResponse;
    }

    @Override
    public AuthResponse refreshToken(String oldToken) throws UserException {
        log.info("Refreshing token");

        // Check if token is blacklisted
        if (isTokenBlacklisted(oldToken)) {
            log.warn("Attempt to refresh blacklisted token");
            throw new UserException("Token is invalid");
        }

        try {
            // Validate the old token (allow expired tokens for refresh)
            String email = null;
            try {
                // Try to get email from token even if expired
                email = jwtProvider.getEmailFromToken(oldToken);

                // If token is not expired, validate it
                if (jwtProvider.validateToken(oldToken)) {
                    log.info("Token is still valid, refreshing anyway");
                }
            } catch (ExpiredJwtException e) {
                // Token is expired, but we can still extract email from claims
                Claims claims = e.getClaims();
                email = claims.getSubject();
                log.info("Refreshing expired token for user: {}", email);
            } catch (Exception e) {
                log.error("Invalid token format: {}", e.getMessage());
                throw new UserException("Invalid token");
            }

            if (email == null || email.trim().isEmpty()) {
                throw new UserException("Could not extract user information from token");
            }

            // Load user details
            UserDetails userDetails = customUserImplementation.loadUserByUsername(email);
            if (userDetails == null) {
                throw new UserException("User not found");
            }

            // Check if user exists in database
            User user = userRepository.findByEmail(email);
            if (user == null) {
                throw new UserException("User not found in database");
            }

            // Blacklist the old token
            blacklistToken(oldToken);

            // Create new authentication
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );

            // Generate new token
            String newJwt = jwtProvider.generateToken(authentication);

            // Update user's last activity
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            // Create response
            AuthResponse authResponse = new AuthResponse();
            authResponse.setJwt(newJwt);
            authResponse.setMessage("Token refreshed successfully");
            authResponse.setUser(UserMapper.toDTO(user));

            log.info("Token refreshed successfully for user: {}", email);
            return authResponse;

        } catch (UserException e) {
            throw e; // Re-throw UserException
        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage());
            throw new UserException("Failed to refresh token: " + e.getMessage());
        }
    }

    @Override
    public void logout(String token) {
        log.info("Logout requested");

        if (token == null || token.trim().isEmpty()) {
            log.warn("Logout called with null or empty token");
            return;
        }

        // Extract token if it has "Bearer " prefix
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        try {
            // Blacklist the token
            blacklistToken(token);

            // Try to get email from token for logging
            String email = null;
            try {
                email = jwtProvider.getEmailFromToken(token);
            } catch (Exception e) {
                // Token might be invalid/expired, but we still blacklist it
                log.debug("Could not extract email from token during logout");
            }

            log.info("Token blacklisted{}", email != null ? " for user: " + email : "");

        } catch (Exception e) {
            log.error("Error during logout: {}", e.getMessage());
        }
    }

    /**
     * Add token to blacklist
     */
    private void blacklistToken(String token) {
        try {
            // Get token expiration date
            Date expirationDate = jwtProvider.getExpirationDateFromToken(token);

            // Only blacklist if token has expiration date
            if (expirationDate != null) {
                tokenBlacklist.put(token, expirationDate);
                log.debug("Token added to blacklist, expires at: {}", expirationDate);

                // Schedule cleanup after token expires
                cleanupBlacklist();
            }
        } catch (Exception e) {
            // If we can't parse the token, still add it with current time
            tokenBlacklist.put(token, new Date());
            log.debug("Added invalid token to blacklist");
        }
    }

    /**
     * Check if token is blacklisted
     */
    private boolean isTokenBlacklisted(String token) {
        if (token == null) return false;

        Date expirationDate = tokenBlacklist.get(token);
        if (expirationDate == null) {
            return false;
        }

        // If token expiration has passed, remove from blacklist
        if (expirationDate.before(new Date())) {
            tokenBlacklist.remove(token);
            return false;
        }

        return true;
    }

    /**
     * Clean up expired tokens from blacklist
     */
    private void cleanupBlacklist() {
        Date now = new Date();
        tokenBlacklist.entrySet().removeIf(entry -> {
            if (entry.getValue().before(now)) {
                log.debug("Removing expired token from blacklist");
                return true;
            }
            return false;
        });
    }

    private Authentication authenticate(String email, String password) throws UserException {
        UserDetails userDetails = customUserImplementation.loadUserByUsername(email);

        if (userDetails == null) {
            log.warn("Login failed - email doesn't exist: {}", email);
            throw new UserException("Invalid email or password");
        }

        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            log.warn("Login failed - password mismatch for: {}", email);
            throw new UserException("Invalid email or password");
        }

        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
    }

    private void validateSignupRequest(UserDto userDto) throws UserException {
        if (userDto.getFirstName() == null || userDto.getFirstName().trim().isEmpty()) {
            throw new UserException("First name is required");
        }

        if (userDto.getFullName() == null || userDto.getFullName().trim().isEmpty()) {
            throw new UserException("Full name is required");
        }

        if (userDto.getEmail() == null || userDto.getEmail().trim().isEmpty()) {
            throw new UserException("Email is required");
        }

        if (userDto.getPassword() == null || userDto.getPassword().trim().isEmpty()) {
            throw new UserException("Password is required");
        }

        // Optional: Password strength validation
        if (userDto.getPassword().length() < 6) {
            throw new UserException("Password must be at least 6 characters");
        }

        // Optional: Email format validation
        if (!userDto.getEmail().contains("@")) {
            throw new UserException("Invalid email format");
        }
    }

    private User createUserFromDto(UserDto userDto) {
        User user = new User();

        // Set required fields
        user.setFirstName(userDto.getFirstName());
        user.setFullName(userDto.getFullName());
        user.setEmail(userDto.getEmail());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));

        // Set optional fields with defaults
        user.setRole(userDto.getRole() != null ? userDto.getRole() : UserRole.USER);
        user.setPhone(userDto.getPhone());

        // Set timestamps
        LocalDateTime now = LocalDateTime.now();
        user.setLastLogin(now);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        return user;
    }

    // Optional: Add a helper method to verify data
    private void logUserDetails(User user) {
        log.debug("User Details - ID: {}, FirstName: {}, FullName: {}, Email: {}, Role: {}",
                user.getId(), user.getFirstName(), user.getFullName(), user.getEmail(), user.getRole());
    }
}