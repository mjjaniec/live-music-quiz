package com.github.mjjaniec.lmq.stores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.mjjaniec.lmq.model.Maestro;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("integration-test")
class MaestroStoreTest {

    @Autowired
    private MaestroStore maestroStore;

    @Test
    void createIfAbsentCreatesNewAccountOnFirstRequest() {
        Maestro created = maestroStore.createIfAbsent("host@example.com");

        assertEquals("host@example.com", created.email());
        assertNotNull(created.id());
    }

    @Test
    void createIfAbsentIsIdempotentForSameEmail() {
        Maestro first = maestroStore.createIfAbsent("host@example.com");
        Maestro second = maestroStore.createIfAbsent("host@example.com");

        assertEquals(first.id(), second.id());
    }

    @Test
    void findByEmailReturnsEmptyWhenAbsent() {
        assertTrue(maestroStore.findByEmail("missing@example.com").isEmpty());
    }

    @Test
    void findByEmailReturnsCreatedAccount() {
        Maestro created = maestroStore.createIfAbsent("host@example.com");

        Optional<Maestro> found = maestroStore.findByEmail("host@example.com");

        assertEquals(Optional.of(created), found);
    }
}
