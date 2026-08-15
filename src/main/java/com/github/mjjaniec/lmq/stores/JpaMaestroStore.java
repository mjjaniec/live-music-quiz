package com.github.mjjaniec.lmq.stores;

import com.github.mjjaniec.lmq.model.Maestro;
import jakarta.transaction.Transactional;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

@Component
public interface JpaMaestroStore extends CrudRepository<MaestroDto, UUID>, MaestroStore {

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
            return mapFromDto(save(dto));
        });
    }

    private Maestro mapFromDto(MaestroDto dto) {
        return new Maestro(dto.getId(), dto.getEmail());
    }
}
