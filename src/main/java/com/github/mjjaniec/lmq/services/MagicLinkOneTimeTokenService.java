package com.github.mjjaniec.lmq.services;

import com.github.mjjaniec.lmq.stores.JpaMagicLinkTokenStore;
import com.github.mjjaniec.lmq.stores.MaestroStore;
import com.github.mjjaniec.lmq.stores.MagicLinkTokenDto;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.ott.GenerateOneTimeTokenRequest;
import org.springframework.security.authentication.ott.OneTimeToken;
import org.springframework.security.authentication.ott.OneTimeTokenAuthenticationToken;
import org.springframework.security.authentication.ott.OneTimeTokenService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@RequiredArgsConstructor
public class MagicLinkOneTimeTokenService implements OneTimeTokenService {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);
    private static final Duration COOLDOWN = Duration.ofSeconds(60);

    private final JpaMagicLinkTokenStore tokenStore;
    private final MaestroStore maestroStore;

    private final Map<String, Instant> emailCooldown = new ConcurrentHashMap<>();
    private final Map<String, Instant> ipCooldown = new ConcurrentHashMap<>();

    @Override
    public OneTimeToken generate(GenerateOneTimeTokenRequest request) {
        String email = request.getUsername();
        String ip = currentRemoteAddress();
        Instant now = Instant.now();

        if (isCoolingDown(emailCooldown, email, now) || isCoolingDown(ipCooldown, ip, now)) {
            return new MagicLinkToken(UUID.randomUUID().toString(), email, now.plus(TOKEN_TTL), true);
        }

        emailCooldown.put(email, now);
        ipCooldown.put(ip, now);
        maestroStore.createIfAbsent(email);

        String tokenValue = UUID.randomUUID().toString();
        Instant expiresAt = now.plus(TOKEN_TTL);
        MagicLinkTokenDto dto = new MagicLinkTokenDto();
        dto.setToken(tokenValue);
        dto.setEmail(email);
        dto.setExpiresAt(expiresAt);
        tokenStore.save(dto);

        return new MagicLinkToken(tokenValue, email, expiresAt, false);
    }

    @Override
    public @Nullable OneTimeToken consume(OneTimeTokenAuthenticationToken authenticationToken) {
        String tokenValue = authenticationToken.getTokenValue();
        return tokenStore
                .findById(tokenValue)
                .map(dto -> {
                    tokenStore.deleteById(tokenValue);
                    if (dto.getExpiresAt().isBefore(Instant.now())) {
                        return null;
                    }
                    return (OneTimeToken) new MagicLinkToken(dto.getToken(), dto.getEmail(), dto.getExpiresAt(), false);
                })
                .orElse(null);
    }

    /** True when {@code token} came from a cooldown-suppressed request — no email should be dispatched for it. */
    public boolean isSuppressed(OneTimeToken token) {
        return token instanceof MagicLinkToken magicLinkToken && magicLinkToken.suppressed();
    }

    private static boolean isCoolingDown(Map<String, Instant> cooldowns, String key, Instant now) {
        Instant last = cooldowns.get(key);
        return last != null && last.plus(COOLDOWN).isAfter(now);
    }

    private static String currentRemoteAddress() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attributes.getRequest().getRemoteAddr();
    }

    private record MagicLinkToken(String tokenValue, String username, Instant expiresAt, boolean suppressed)
            implements OneTimeToken {
        @Override
        public String getTokenValue() {
            return tokenValue;
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public Instant getExpiresAt() {
            return expiresAt;
        }
    }
}
