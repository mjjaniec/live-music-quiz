package com.github.mjjaniec.stores;

import com.github.mjjaniec.model.Player;
import com.google.common.collect.Streams;
import jakarta.transaction.Transactional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public interface JpaPlayeOffStore extends CrudRepository<PlayOffDto, String>, PlayOffStore {

    @Override
    default void clearPlayOffs() {
        deleteAll();
    }

    @Override
    default Optional<Integer> getPlayOff(Player player) {
        return findById(player.name()).map(PlayOffDto::getValue);
    }

    @Override
    default void savePlayOff(Player player, int value) {
        PlayOffDto dto = new PlayOffDto();
        dto.setPlayer(player.name());
        dto.setValue(value);
        save(dto);
    }

}
