package com.github.mjjaniec.lmq.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.mjjaniec.lmq.stores.JpaMagicLinkTokenStore;
import com.github.mjjaniec.lmq.stores.MagicLinkTokenDto;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("integration-test")
class MagicLinkTokenCleanupTaskTest {

    @Autowired
    private JpaMagicLinkTokenStore tokenStore;

    @Test
    void purgeExpiredTokensRemovesOnlyExpiredRows() {
        MagicLinkTokenDto expired = new MagicLinkTokenDto();
        expired.setToken("expired-token");
        expired.setEmail("expired@example.com");
        expired.setExpiresAt(Instant.now().minusSeconds(60));
        tokenStore.save(expired);

        MagicLinkTokenDto valid = new MagicLinkTokenDto();
        valid.setToken("valid-token");
        valid.setEmail("valid@example.com");
        valid.setExpiresAt(Instant.now().plusSeconds(600));
        tokenStore.save(valid);

        new MagicLinkTokenCleanupTask(tokenStore).purgeExpiredTokens();

        assertFalse(tokenStore.findById("expired-token").isPresent());
        assertTrue(tokenStore.findById("valid-token").isPresent());
    }
}
