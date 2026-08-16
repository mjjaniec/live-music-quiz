package com.github.mjjaniec.lmq.api;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Matches only when {@code RAILWAY_SERVICE_NAME} is absent, so test-only beans stay disabled even
 * if {@code integration-test} ever leaks into a Railway deployment's active profiles.
 */
class NotOnRailwayCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return !context.getEnvironment().containsProperty("RAILWAY_SERVICE_NAME");
    }
}
