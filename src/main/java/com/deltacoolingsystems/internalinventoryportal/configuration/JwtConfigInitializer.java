package com.deltacoolingsystems.internalinventoryportal.configuration;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfigInitializer {

    @PostConstruct
    public void init() {
        // Print security configuration on startup
        JwtConstant.printSecuritySummary();

        // Validate security configuration
        if (JwtConstant.isUsingDefaultSecret()) {
            System.err.println("\n⚠️ ⚠️ ⚠️  SECURITY WARNING ⚠️ ⚠️ ⚠️");
            System.err.println("Using default JWT secret!");
            System.err.println("In production, set JWT_SECRET environment variable!");
            System.err.println("Example: export JWT_SECRET='your-very-long-secure-random-secret-key-here'");
            System.err.println("⚠️ ⚠️ ⚠️ ⚠️ ⚠️ ⚠️ ⚠️ ⚠️ ⚠️ ⚠️ ⚠️ ⚠️ ⚠️ ⚠️ ⚠️\n");
        }
    }
}