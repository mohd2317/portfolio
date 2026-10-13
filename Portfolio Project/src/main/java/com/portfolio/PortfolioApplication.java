package com.portfolio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point. Runs as an executable JAR (embedded Tomcat) or, when built with
 * {@code -Pwar}, as a WAR through {@link ServletInitializer}.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class PortfolioApplication {

    public static void main(String[] args) {
        SpringApplication.run(PortfolioApplication.class, args);
    }
}
