package com.github.mjjaniec.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application")
public record ApplicationConfig(boolean enableFrontRouting) {
}
