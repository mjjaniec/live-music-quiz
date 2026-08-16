package com.github.mjjaniec.lmq.services;

import com.github.mjjaniec.lmq.stores.JpaMagicLinkTokenStore;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MagicLinkTokenCleanupTask {

    private final JpaMagicLinkTokenStore tokenStore;

    @Scheduled(fixedDelay = 15, initialDelay = 15, timeUnit = TimeUnit.MINUTES)
    void purgeExpiredTokens() {
        long deleted = tokenStore.deleteByExpiresAtBefore(Instant.now());
        if (deleted > 0) {
            log.info("Purged {} expired magic-link token(s)", deleted);
        }
    }
}
