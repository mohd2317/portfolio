package com.portfolio;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Bootstraps the application when it is deployed as a WAR into an external
 * servlet container (Tomcat, WildFly, Liberty, JBoss EAP...).
 */
public class ServletInitializer extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(PortfolioApplication.class);
    }
}
