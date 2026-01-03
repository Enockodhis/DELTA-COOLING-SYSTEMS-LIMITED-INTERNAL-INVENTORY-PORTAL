package com.deltacoolingsystems.internalinventoryportal.configuration;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Base64;

@Component
public class JwtValidator extends OncePerRequestFilter {

    private final String jwtSecret = "super-secure-384-bit-jwt-secret-for-hs384-algorithm-here!";

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, "HmacSHA384");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || authHeader.isBlank()) {
            // No Authorization header, continue filter chain
            filterChain.doFilter(request, response);
            return;
        }

        authHeader = authHeader.trim();

        if (!authHeader.startsWith("Bearer ")) {
            // Not a Bearer token, continue filter chain
            filterChain.doFilter(request, response);
            return;
        }

        // Extract and clean the token
        String jwt = authHeader.substring(7).trim();

        // Clean the token - remove ALL whitespace and control characters
        jwt = cleanJwtToken(jwt);

        // Log for debugging (remove in production)
        logger.debug("Cleaned JWT token: " + jwt.substring(0, Math.min(20, jwt.length())) + "...");

        // Validate token format
        if (!isValidJwtFormat(jwt)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT format");
            return;
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(jwt)
                    .getPayload();

            String email = claims.getSubject();
            String authorities = String.valueOf(claims.get("authorities"));

            List<GrantedAuthority> auths = AuthorityUtils.commaSeparatedStringToAuthorityList(authorities);

            Authentication authentication = new UsernamePasswordAuthenticationToken(email, null, auths);
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (SignatureException e) {
            // Invalid signature
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT signature");
            return;
        } catch (Exception e) {
            // Any other JWT exception (expired, malformed, etc.)
            logger.error("JWT validation error: " + e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired JWT");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Clean JWT token by removing control characters and invalid characters
     */
    private String cleanJwtToken(String token) {
        if (token == null) {
            return "";
        }

        // Remove all control characters (including CTRL-CHAR, code 5)
        String cleaned = token.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");

        // Remove all whitespace (including \r, \n, \t)
        cleaned = cleaned.replaceAll("\\s+", "");

        // Remove any non-Base64URL characters
        cleaned = cleaned.replaceAll("[^A-Za-z0-9-_=]", "");

        return cleaned;
    }

    /**
     * Validate JWT format
     */
    private boolean isValidJwtFormat(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        // Split by dots and check for exactly 3 parts
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return false;
        }

        // Check each part is valid Base64URL
        for (String part : parts) {
            if (!isValidBase64Url(part)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check if string is valid Base64URL
     */
    private boolean isValidBase64Url(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        // Base64URL pattern: A-Z, a-z, 0-9, -, _, =
        return str.matches("^[A-Za-z0-9-_=]+$");
    }
}