package com.github.mjjaniec.lmq.stores;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

@Component
public interface JpaMagicLinkTokenStore extends CrudRepository<MagicLinkTokenDto, String> {

    /**
     * Locks the row for the duration of the caller's transaction, so a concurrent consume of the same token
     * blocks until the first transaction commits (and the row is gone) rather than racing to read it twice.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MagicLinkTokenDto> findByToken(String token);

    /** Purges expired, never-consumed tokens; returns the number of rows removed. */
    long deleteByExpiresAtBefore(Instant instant);
}
