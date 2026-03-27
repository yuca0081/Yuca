package org.yuca.ai;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Test application configuration for yuca-ai module tests.
 * This avoids circular dependencies with yuca-app module.
 */
@SpringBootApplication(scanBasePackages = "org.yuca")
@ConfigurationPropertiesScan(basePackages = "org.yuca")
public class TestApplication {
}
