package com.deltacoolingsystems.internalinventoryportal;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
@Profile("test") // This ensures it only runs when 'test' profile is active
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    MySQLContainer<?> mysqlContainer() {
        // You can change version if needed (e.g., mysql:8.0)
        return new MySQLContainer<>(DockerImageName.parse("mysql:latest"))
                .withDatabaseName("deltacoolingdb")
                .withUsername("enock")
                .withPassword("E35623101009k.");
    }

}
