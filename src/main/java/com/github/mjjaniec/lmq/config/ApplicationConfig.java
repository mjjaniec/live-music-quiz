package com.github.mjjaniec.lmq.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application")
public record ApplicationConfig(boolean enableFrontRouting, boolean testData) {
}
