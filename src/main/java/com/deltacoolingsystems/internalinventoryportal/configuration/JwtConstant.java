package com.deltacoolingsystems.internalinventoryportal.configuration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JwtConstant {

    // ==============================================
    // Environment Variable Names
    // ==============================================
    private static final String ENV_JWT_SECRET = "JWT_SECRET";
    private static final String ENV_JWT_ACCESS_EXPIRATION = "JWT_ACCESS_EXPIRATION_HOURS";
    private static final String ENV_JWT_REFRESH_EXPIRATION = "JWT_REFRESH_EXPIRATION_DAYS";

    // ==============================================
    // Default Values (Fallback if env vars not set)
    // ==============================================
    private static final String DEFAULT_JWT_SECRET = "super-secure-384-bit-jwt-secret-for-hs384-algorithm-here!";
    private static final long DEFAULT_ACCESS_EXPIRATION_HOURS = 24;    // 24 hours
    private static final long DEFAULT_REFRESH_EXPIRATION_DAYS = 7;     // 7 days

    // ==============================================
    // Runtime Configuration (Loaded from env vars)
    // ==============================================

    // JWT Secret Key - Load from environment variable with fallback
    // WARNING: In production, ALWAYS use environment variable!
    public static final String JWT_SECRET = loadJwtSecret();

    // Token expiration times in milliseconds
    public static final long ACCESS_TOKEN_EXPIRATION = loadAccessTokenExpiration();
    public static final long REFRESH_TOKEN_EXPIRATION = loadRefreshTokenExpiration();

    // ==============================================
    // Static Configuration (Can be hardcoded)
    // ==============================================

    // HTTP Header where JWT token is expected
    public static final String JWT_HEADER = "Authorization";

    // Maximum age for token refresh (how old a token can be to still be refreshable)
    public static final long MAX_REFRESH_AGE = 7 * 24 * 60 * 60 * 1000L; // 7 days

    // Token prefixes
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final int TOKEN_PREFIX_LENGTH = TOKEN_PREFIX.length(); // "Bearer ".length() = 7

    // Token types (for future use if implementing refresh tokens)
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    // Claim names
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_AUTHORITIES = "authorities";
    public static final String CLAIM_TOKEN_TYPE = "token_type";
    public static final String CLAIM_ISSUED_AT = "iat";
    public static final String CLAIM_EXPIRATION = "exp";
    public static final String CLAIM_JTI = "jti"; // JWT ID for token tracking

    // Error messages
    public static final String ERROR_INVALID_TOKEN = "Invalid JWT token";
    public static final String ERROR_EXPIRED_TOKEN = "JWT token has expired";
    public static final String ERROR_MALFORMED_TOKEN = "Malformed JWT token";
    public static final String ERROR_UNSUPPORTED_TOKEN = "Unsupported JWT token";
    public static final String ERROR_EMPTY_TOKEN = "JWT token is empty or null";
    public static final String ERROR_MISSING_SECRET = "JWT secret is not configured";

    // Security
    public static final int MIN_SECRET_KEY_LENGTH = 48; // Minimum bytes for HS384
    public static final int RECOMMENDED_SECRET_KEY_LENGTH = 64; // Recommended for production

    // Blacklist cleanup interval in milliseconds (1 hour)
    public static final long BLACKLIST_CLEANUP_INTERVAL = 60 * 60 * 1000L;

    // Rate limiting for token refresh (max attempts per hour)
    public static final int MAX_REFRESH_ATTEMPTS_PER_HOUR = 10;

    // Issuer claim (optional)
    public static final String ISSUER = "DeltaCoolingSystems-InternalInventoryPortal";

    // Audience claim (optional)
    public static final String AUDIENCE = "internal-inventory-users";

    private JwtConstant() {
        // Private constructor to prevent instantiation
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ==============================================
    // Environment Variable Loaders
    // ==============================================

    /**
     * Load JWT secret from environment variable with fallback
     * Logs warning if using default secret in non-development environment
     */
    private static String loadJwtSecret() {
        String secret = System.getenv(ENV_JWT_SECRET);

        if (secret == null || secret.trim().isEmpty()) {
            log.warn("⚠️  JWT_SECRET environment variable not set. Using default secret.");
            log.warn("⚠️  IN PRODUCTION: Set JWT_SECRET environment variable!");
            secret = DEFAULT_JWT_SECRET;
        } else {
            log.info("✅ JWT_SECRET loaded from environment variable");
        }

        // Validate secret length
        if (secret.getBytes().length < MIN_SECRET_KEY_LENGTH) {
            log.error("❌ JWT secret is too short. Minimum {} bytes required for HS384 algorithm.", MIN_SECRET_KEY_LENGTH);
            throw new SecurityException(
                    String.format("JWT secret must be at least %d bytes for HS384 algorithm",
                            MIN_SECRET_KEY_LENGTH)
            );
        }

        if (secret.getBytes().length >= RECOMMENDED_SECRET_KEY_LENGTH) {
            log.info("✅ JWT secret meets recommended security length");
        } else {
            log.warn("⚠️  JWT secret length is acceptable but not optimal. Recommended: {}+ bytes",
                    RECOMMENDED_SECRET_KEY_LENGTH);
        }

        return secret;
    }

    /**
     * Load access token expiration from environment variable
     */
    private static long loadAccessTokenExpiration() {
        String envValue = System.getenv(ENV_JWT_ACCESS_EXPIRATION);

        if (envValue == null || envValue.trim().isEmpty()) {
            log.info("Using default access token expiration: {} hours", DEFAULT_ACCESS_EXPIRATION_HOURS);
            return DEFAULT_ACCESS_EXPIRATION_HOURS * 60 * 60 * 1000L;
        }

        try {
            long hours = Long.parseLong(envValue);
            log.info("Access token expiration loaded from environment: {} hours", hours);
            return hours * 60 * 60 * 1000L;
        } catch (NumberFormatException e) {
            log.error("Invalid JWT_ACCESS_EXPIRATION_HOURS value: {}. Using default.", envValue);
            return DEFAULT_ACCESS_EXPIRATION_HOURS * 60 * 60 * 1000L;
        }
    }

    /**
     * Load refresh token expiration from environment variable
     */
    private static long loadRefreshTokenExpiration() {
        String envValue = System.getenv(ENV_JWT_REFRESH_EXPIRATION);

        if (envValue == null || envValue.trim().isEmpty()) {
            log.info("Using default refresh token expiration: {} days", DEFAULT_REFRESH_EXPIRATION_DAYS);
            return DEFAULT_REFRESH_EXPIRATION_DAYS * 24 * 60 * 60 * 1000L;
        }

        try {
            long days = Long.parseLong(envValue);
            log.info("Refresh token expiration loaded from environment: {} days", days);
            return days * 24 * 60 * 60 * 1000L;
        } catch (NumberFormatException e) {
            log.error("Invalid JWT_REFRESH_EXPIRATION_DAYS value: {}. Using default.", envValue);
            return DEFAULT_REFRESH_EXPIRATION_DAYS * 24 * 60 * 60 * 1000L;
        }
    }

    /**
     * Check if running in production (simplified check)
     */
    public static boolean isProduction() {
        String env = System.getenv("SPRING_PROFILES_ACTIVE");
        return env != null && (env.contains("prod") || env.contains("production"));
    }

    /**
     * Validate if the secret key meets minimum security requirements
     */
    public static boolean isSecretKeyValid() {
        return JWT_SECRET != null && JWT_SECRET.getBytes().length >= MIN_SECRET_KEY_LENGTH;
    }

    /**
     * Check if using default secret (security warning)
     */
    public static boolean isUsingDefaultSecret() {
        return JWT_SECRET.equals(DEFAULT_JWT_SECRET);
    }

    /**
     * Get the secret key bytes with validation
     */
    public static byte[] getSecretKeyBytes() {
        if (!isSecretKeyValid()) {
            throw new SecurityException(
                    String.format("JWT secret key must be at least %d bytes for HS384 algorithm",
                            MIN_SECRET_KEY_LENGTH)
            );
        }

        if (isUsingDefaultSecret() && isProduction()) {
            log.error("❌ CRITICAL SECURITY ISSUE: Using default JWT secret in production!");
            throw new SecurityException(
                    "Default JWT secret detected in production. Set JWT_SECRET environment variable!"
            );
        }

        return JWT_SECRET.getBytes();
    }

    /**
     * Print security configuration summary (useful for startup logs)
     */
    public static void printSecuritySummary() {
        log.info("🔐 JWT Security Configuration:");
        log.info("   Secret Source: {}", isUsingDefaultSecret() ? "DEFAULT (⚠️ Warning)" : "Environment Variable");
        log.info("   Secret Length: {} bytes", JWT_SECRET.getBytes().length);
        log.info("   Access Token Expiration: {} hours", ACCESS_TOKEN_EXPIRATION / (60 * 60 * 1000L));
        log.info("   Refresh Token Expiration: {} days", REFRESH_TOKEN_EXPIRATION / (24 * 60 * 60 * 1000L));

        if (isUsingDefaultSecret()) {
            log.warn("   ⚠️  SECURITY WARNING: Using default JWT secret!");
            log.warn("   ⚠️  Set JWT_SECRET environment variable in production!");
        }
    }
}