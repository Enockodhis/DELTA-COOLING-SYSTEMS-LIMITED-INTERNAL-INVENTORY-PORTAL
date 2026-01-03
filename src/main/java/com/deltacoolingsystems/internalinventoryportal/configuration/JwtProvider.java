package com.deltacoolingsystems.internalinventoryportal.configuration;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class JwtProvider {

    private final SecretKey key;

    public JwtProvider(JwtConfigInitializer configInitializer) {

        // Initialize with validated secret key from JwtConstant
        try {
            this.key = Keys.hmacShaKeyFor(JwtConstant.getSecretKeyBytes());
            log.info("✅ JwtProvider initialized successfully");
            log.debug("JWT Algorithm: HMAC-SHA384, Key length: {} bytes",
                    JwtConstant.JWT_SECRET.getBytes().length);
        } catch (Exception e) {
            log.error("❌ Failed to initialize JwtProvider: {}", e.getMessage());
            throw new SecurityException("JWT initialization failed: " + e.getMessage(), e);
        }
    }

    /**
     * Generate access token from Authentication object
     */
    public String generateToken(Authentication authentication) {
        validateAuthentication(authentication);

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        String roles = populateAuthorities(authorities);
        String email = authentication.getName();
        String jti = generateJti(); // Unique JWT ID for tracking

        log.debug("Generating access token for: {}, Roles: {}", email, roles);

        return buildToken(email, roles, JwtConstant.ACCESS_TOKEN_EXPIRATION,
                JwtConstant.TOKEN_TYPE_ACCESS, jti);
    }

    /**
     * Generate token with custom claims
     */
    public String generateToken(String email, String authorities, long expirationMs, String tokenType) {
        validateEmail(email);
        validateAuthorities(authorities);

        String jti = generateJti();
        log.debug("Generating {} token for: {}, Expires in: {}ms",
                tokenType, email, expirationMs);

        return buildToken(email, authorities, expirationMs, tokenType, jti);
    }

    /**
     * Generate refresh token
     */
    public String generateRefreshToken(String email, String authorities) {
        validateEmail(email);

        String jti = generateJti();
        if (log.isDebugEnabled()) {
            log.debug("Generating refresh token for: {}", email);
        }

        return buildToken(email, authorities, JwtConstant.REFRESH_TOKEN_EXPIRATION,
                JwtConstant.TOKEN_TYPE_REFRESH, jti);
    }

    /**
     * Generate refresh token from Authentication
     */
    public String generateRefreshToken(Authentication authentication) {
        validateAuthentication(authentication);

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        String roles = populateAuthorities(authorities);
        String email = authentication.getName();
        String jti = generateJti();

        log.debug("Generating refresh token for: {}", email);

        return buildToken(email, roles, JwtConstant.REFRESH_TOKEN_EXPIRATION,
                JwtConstant.TOKEN_TYPE_REFRESH, jti);
    }

    /**
     * Core token building method
     */
    private String buildToken(String email, String authorities, long expirationMs,
                              String tokenType, String jti) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .id(jti) // JWT ID for tracking/blacklisting
                .issuer(JwtConstant.ISSUER)
                .audience().add(JwtConstant.AUDIENCE).and()
                .subject(email)
                .issuedAt(now)
                .expiration(expiration)
                .claim(JwtConstant.CLAIM_EMAIL, email)
                .claim(JwtConstant.CLAIM_AUTHORITIES, authorities)
                .claim(JwtConstant.CLAIM_TOKEN_TYPE, tokenType)
                .signWith(key)
                .compact();
    }

    /**
     * Extract email from token
     */
    public String getEmailFromToken(String jwt) {
        jwt = extractToken(jwt);

        try {
            Claims claims = parseToken(jwt);
            return getClaimAsString(claims, JwtConstant.CLAIM_EMAIL);
        } catch (ExpiredJwtException e) {
            // Token is expired, but we can still extract claims
            log.debug("Extracting email from expired token");
            return getClaimAsString(e.getClaims(), JwtConstant.CLAIM_EMAIL);
        } catch (Exception e) {
            log.error("Error extracting email from token: {}", e.getMessage());
            throw new JwtException(JwtConstant.ERROR_INVALID_TOKEN, e);
        }
    }

    /**
     * Extract JWT ID from token
     */
    public String getJtiFromToken(String token) {
        token = extractToken(token);

        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getId();
        } catch (Exception e) {
            log.debug("Could not extract JTI from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Parse token and return claims with validation
     */
    private Claims parseToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException(JwtConstant.ERROR_EMPTY_TOKEN);
        }

        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(JwtConstant.ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Get all claims from token (even if expired)
     */
    public Claims getClaimsFromToken(String token) {
        token = extractToken(token);

        try {
            return parseToken(token);
        } catch (ExpiredJwtException e) {
            // Return claims even if token is expired (useful for refresh)
            log.debug("Token expired but returning claims for refresh");
            return e.getClaims();
        } catch (Exception e) {
            log.error("Error parsing token claims: {}", e.getMessage());
            throw new JwtException(JwtConstant.ERROR_INVALID_TOKEN, e);
        }
    }

    /**
     * Get expiration date from token
     */
    public Date getExpirationDateFromToken(String token) {
        token = extractToken(token);

        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getExpiration();
        } catch (Exception e) {
            log.error("Error getting expiration from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get issued at date from token
     */
    public Date getIssuedAtFromToken(String token) {
        token = extractToken(token);

        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getIssuedAt();
        } catch (Exception e) {
            log.error("Error getting issued at from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get authorities from token
     */
    public String getAuthoritiesFromToken(String token) {
        token = extractToken(token);

        try {
            Claims claims = getClaimsFromToken(token);
            return getClaimAsString(claims, JwtConstant.CLAIM_AUTHORITIES);
        } catch (Exception e) {
            log.error("Error getting authorities from token: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Get token type from token
     */
    public String getTokenType(String token) {
        token = extractToken(token);

        try {
            Claims claims = getClaimsFromToken(token);
            String tokenType = getClaimAsString(claims, JwtConstant.CLAIM_TOKEN_TYPE);
            return tokenType != null ? tokenType : JwtConstant.TOKEN_TYPE_ACCESS;
        } catch (Exception e) {
            log.error("Error getting token type: {}", e.getMessage());
            return JwtConstant.TOKEN_TYPE_ACCESS;
        }
    }

    /**
     * Validate token (check if not expired and properly signed)
     */
    public boolean validateToken(String token) {
        token = extractToken(token);

        if (token == null || token.trim().isEmpty()) {
            log.debug("Token validation failed: empty token");
            return false;
        }

        try {
            parseToken(token);
            log.debug("Token validation successful");
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("Token validation failed: expired");
            return false;
        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if token is expired (without throwing exception)
     */
    public boolean isTokenExpired(String token) {
        token = extractToken(token);

        try {
            Date expiration = getExpirationDateFromToken(token);
            if (expiration == null) {
                return true;
            }
            boolean expired = expiration.before(new Date());
            if (expired) {
                log.debug("Token is expired");
            }
            return expired;
        } catch (Exception e) {
            log.error("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Check if token can be refreshed (not too old)
     */
    public boolean canTokenBeRefreshed(String token) {
        token = extractToken(token);

        try {
            Claims claims = getClaimsFromToken(token);
            Date issuedAt = claims.getIssuedAt();
            Date now = new Date();

            // Allow refresh if token was issued within MAX_REFRESH_AGE
            boolean canRefresh = (now.getTime() - issuedAt.getTime()) < JwtConstant.MAX_REFRESH_AGE;

            if (!canRefresh) {
                log.debug("Token cannot be refreshed: too old");
            }

            return canRefresh;
        } catch (Exception e) {
            log.error("Error checking if token can be refreshed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Refresh a token (create new token with same claims but new expiration)
     */
    public String refreshToken(String oldToken) {
        oldToken = extractToken(oldToken);

        try {
            if (!canTokenBeRefreshed(oldToken)) {
                throw new JwtException("Token is too old to refresh");
            }

            Claims claims = getClaimsFromToken(oldToken);
            String email = getClaimAsString(claims, JwtConstant.CLAIM_EMAIL);
            String authorities = getClaimAsString(claims, JwtConstant.CLAIM_AUTHORITIES);
            String tokenType = getTokenType(oldToken);

            // Generate new token with same claims but new expiration
            long expiration = tokenType.equals(JwtConstant.TOKEN_TYPE_REFRESH)
                    ? JwtConstant.REFRESH_TOKEN_EXPIRATION
                    : JwtConstant.ACCESS_TOKEN_EXPIRATION;

            log.info("Refreshing {} token for: {}", tokenType, email);
            return generateToken(email, authorities, expiration, tokenType);
        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage());
            throw new JwtException("Cannot refresh token: " + e.getMessage(), e);
        }
    }

    /**
     * Get time until token expires in milliseconds
     */
    public long getTimeUntilExpiration(String token) {
        token = extractToken(token);

        try {
            Date expiration = getExpirationDateFromToken(token);
            if (expiration == null) {
                return 0;
            }
            long timeUntilExpiration = expiration.getTime() - System.currentTimeMillis();
            return Math.max(0, timeUntilExpiration); // Return 0 if already expired
        } catch (Exception e) {
            log.error("Error getting time until expiration: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Check if token is about to expire (within threshold)
     */
    public boolean isTokenAboutToExpire(String token, long thresholdMs) {
        long timeUntilExpiration = getTimeUntilExpiration(token);
        boolean aboutToExpire = timeUntilExpiration > 0 && timeUntilExpiration < thresholdMs;

        if (aboutToExpire) {
            log.debug("Token expires in {}ms (threshold: {}ms)", timeUntilExpiration, thresholdMs);
        }

        return aboutToExpire;
    }

    /**
     * Check if token should be refreshed (expiring soon)
     */
    public boolean shouldRefreshToken(String token) {
        // Refresh if token expires within 5 minutes
        return isTokenAboutToExpire(token, 5 * 60 * 1000L);
    }

    private String populateAuthorities(Collection<? extends GrantedAuthority> authorities) {
        Set<String> auths = new HashSet<>();
        for (GrantedAuthority grantedAuthority : authorities) {
            auths.add(grantedAuthority.getAuthority());
        }
        return String.join(",", auths);
    }

    /**
     * Extract token from "Bearer <token>" format
     */
    private String extractToken(String token) {
        if (token == null) {
            return null;
        }
        if (token.startsWith(JwtConstant.TOKEN_PREFIX)) {
            return token.substring(JwtConstant.TOKEN_PREFIX_LENGTH);
        }
        return token;
    }

    /**
     * Add "Bearer " prefix to token
     */
    public String addBearerPrefix(String token) {
        if (token == null) {
            return null;
        }
        if (!token.startsWith(JwtConstant.TOKEN_PREFIX)) {
            return JwtConstant.TOKEN_PREFIX + token;
        }
        return token;
    }

    /**
     * Validate token and return detailed result
     */
    public TokenValidationResult validateTokenDetailed(String token) {
        token = extractToken(token);

        if (token == null || token.trim().isEmpty()) {
            return new TokenValidationResult(false, JwtConstant.ERROR_EMPTY_TOKEN, null, null, null);
        }

        try {
            Claims claims = parseToken(token);
            return new TokenValidationResult(true, "Valid token",
                    getClaimAsString(claims, JwtConstant.CLAIM_EMAIL),
                    claims.getExpiration(),
                    claims.getId());
        } catch (ExpiredJwtException e) {
            return new TokenValidationResult(false, JwtConstant.ERROR_EXPIRED_TOKEN,
                    getClaimAsString(e.getClaims(), JwtConstant.CLAIM_EMAIL),
                    e.getClaims().getExpiration(),
                    e.getClaims().getId());
        } catch (Exception e) {
            return new TokenValidationResult(false,
                    JwtConstant.ERROR_INVALID_TOKEN + ": " + e.getMessage(),
                    null, null, null);
        }
    }

    /**
     * Get token information summary
     */
    public TokenInfo getTokenInfo(String token) {
        token = extractToken(token);

        if (token == null) {
            return new TokenInfo(null, null, null, null, null, false, "No token");
        }

        try {
            Claims claims = getClaimsFromToken(token);
            boolean expired = isTokenExpired(token);

            return new TokenInfo(
                    getClaimAsString(claims, JwtConstant.CLAIM_EMAIL),
                    getClaimAsString(claims, JwtConstant.CLAIM_AUTHORITIES),
                    getClaimAsString(claims, JwtConstant.CLAIM_TOKEN_TYPE),
                    claims.getIssuedAt(),
                    claims.getExpiration(),
                    !expired,
                    expired ? "Expired" : "Valid"
            );
        } catch (Exception e) {
            return new TokenInfo(null, null, null, null, null, false, "Invalid: " + e.getMessage());
        }
    }

    // ==============================================
    // Helper Methods
    // ==============================================

    private String generateJti() {
        return UUID.randomUUID().toString();
    }

    private String getClaimAsString(Claims claims, String claimName) {
        Object value = claims.get(claimName);
        return value != null ? String.valueOf(value) : null;
    }

    private void validateAuthentication(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication cannot be null");
        }
        if (authentication.getName() == null || authentication.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Authentication name cannot be empty");
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        if (!email.contains("@")) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }

    private void validateAuthorities(String authorities) {
        if (authorities == null) {
            throw new IllegalArgumentException("Authorities cannot be null");
        }
    }

    // ==============================================
    // Record Classes for Structured Responses
    // ==============================================

    /**
     * Detailed token validation results
     */
    public record TokenValidationResult(
            boolean valid,
            String message,
            String email,
            Date expiration,
            String jti
    ) {}

    /**
     * Comprehensive token information
     */
    public record TokenInfo(
            String email,
            String authorities,
            String tokenType,
            Date issuedAt,
            Date expiration,
            boolean valid,
            String status
    ) {}
}