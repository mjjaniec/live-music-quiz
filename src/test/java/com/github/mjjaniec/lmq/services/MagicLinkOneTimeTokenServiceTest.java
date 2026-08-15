package com.github.mjjaniec.lmq.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.mjjaniec.lmq.stores.JpaMagicLinkTokenStore;
import com.github.mjjaniec.lmq.stores.MaestroStore;
import com.github.mjjaniec.lmq.stores.MagicLinkTokenDto;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.ott.GenerateOneTimeTokenRequest;
import org.springframework.security.authentication.ott.OneTimeToken;
import org.springframework.security.authentication.ott.OneTimeTokenAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@DataJpaTest
@ActiveProfiles("integration-test")
class MagicLinkOneTimeTokenServiceTest {

    @Autowired
    private JpaMagicLinkTokenStore tokenStore;

    @Autowired
    private MaestroStore maestroStore;

    private MagicLinkOneTimeTokenService service;

    @BeforeEach
    void setUp() {
        service = new MagicLinkOneTimeTokenService(tokenStore, maestroStore);
        bindRequest("192.0.2.1");
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private static void bindRequest(String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void generateCreatesAccountAndToken() {
        OneTimeToken token = service.generate(new GenerateOneTimeTokenRequest("new@example.com"));

        assertFalse(service.isSuppressed(token));
        assertEquals("new@example.com", token.getUsername());
        assertTrue(maestroStore.findByEmail("new@example.com").isPresent());
        assertTrue(token.getExpiresAt().isAfter(Instant.now().plus(Duration.ofMinutes(29))));
    }

    @Test
    void consumeSucceedsOnceAndFailsOnReplay() {
        OneTimeToken generated = service.generate(new GenerateOneTimeTokenRequest("replay@example.com"));

        OneTimeToken consumed =
                service.consume(OneTimeTokenAuthenticationToken.unauthenticated(generated.getTokenValue()));
        assertNotNull(consumed);
        assertEquals("replay@example.com", consumed.getUsername());

        OneTimeToken replay =
                service.consume(OneTimeTokenAuthenticationToken.unauthenticated(generated.getTokenValue()));
        assertNull(replay);
    }

    @Test
    void consumeRejectsExpiredToken() {
        OneTimeToken generated = service.generate(new GenerateOneTimeTokenRequest("expired@example.com"));
        MagicLinkTokenDto dto = tokenStore.findById(generated.getTokenValue()).orElseThrow();
        dto.setExpiresAt(Instant.now().minusSeconds(1));
        tokenStore.save(dto);

        OneTimeToken consumed =
                service.consume(OneTimeTokenAuthenticationToken.unauthenticated(generated.getTokenValue()));
        assertNull(consumed);
    }

    @Test
    void perEmailCooldownSuppressesRepeatRequestWithinWindow() {
        OneTimeToken first = service.generate(new GenerateOneTimeTokenRequest("cooldown@example.com"));
        OneTimeToken second = service.generate(new GenerateOneTimeTokenRequest("cooldown@example.com"));

        assertFalse(service.isSuppressed(first));
        assertTrue(service.isSuppressed(second));
    }

    @Test
    void perIpCooldownSuppressesDistinctEmailFromSameIp() {
        OneTimeToken first = service.generate(new GenerateOneTimeTokenRequest("ip-a@example.com"));
        OneTimeToken second = service.generate(new GenerateOneTimeTokenRequest("ip-b@example.com"));

        assertFalse(service.isSuppressed(first));
        assertTrue(service.isSuppressed(second));
    }
}
