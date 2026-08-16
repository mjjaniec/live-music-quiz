package com.github.mjjaniec.lmq.stores;

import com.github.mjjaniec.lmq.model.Maestro;
import java.util.Optional;

public interface MaestroStore {
    Optional<Maestro> findByEmail(String email);

    Maestro createIfAbsent(String email);
}
