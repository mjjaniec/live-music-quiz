package com.github.mjjaniec.lmq.stores;

import com.github.mjjaniec.lmq.model.Maestro;
import jakarta.transaction.Transactional;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

@Component
public interface JpaMaestroStore extends JpaRepository<MaestroDto, UUID>, MaestroStore {

    Optional<MaestroDto> findOneByEmail(String email);

    @Override
    default Optional<Maestro> findByEmail(String email) {
        return findOneByEmail(email).map(this::mapFromDto);
    }

    @Override
    @Transactional
    default Maestro createIfAbsent(String email) {
        return findOneByEmail(email).map(this::mapFromDto).orElseGet(() -> {
            MaestroDto dto = new MaestroDto();
            dto.setId(UUID.randomUUID());
            dto.setEmail(email);
            try {
                return mapFromDto(saveAndFlush(dto));
            } catch (DataIntegrityViolationException e) {
                // Lost a create-if-absent race to a concurrent request for the same email — the
                // other request's row now exists, so fall back to reading it instead of failing.
                return findOneByEmail(email).map(this::mapFromDto).orElseThrow(() -> e);
            }
        });
    }

    private Maestro mapFromDto(MaestroDto dto) {
        return new Maestro(dto.getId(), dto.getEmail());
    }
}
