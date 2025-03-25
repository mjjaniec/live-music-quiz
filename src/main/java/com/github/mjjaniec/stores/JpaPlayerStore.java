package com.github.mjjaniec.stores;

import com.github.mjjaniec.model.Player;
import com.google.common.collect.Streams;
import jakarta.transaction.Transactional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface JpaPlayerStore extends CrudRepository<PlayerDto, String>, PlayerStore {
    @Override
    @Transactional
    default boolean addPlayer(String name) {
        Player player = new Player(name);
        if (hasPlayer(player)) {
            return false;
        }
        save(mapToDto(player));
        return true;
    }

    @Override
    default boolean hasPlayer(Player player) {
        return existsById(player.name());
    }

    @Override
    default List<Player> getPlayers() {
        return Streams.stream(findAll()).map(this::mapFromDto).toList();
    }

    @Override
    default void removePlayer(Player player) {
        delete(mapToDto(player));
    }

    @Override
    default void clearPlayers() {
        deleteAll();
    }

    private PlayerDto mapToDto(Player player) {
        PlayerDto result = new PlayerDto();
        result.setName(player.name());
        return result;
    }

    private Player mapFromDto(PlayerDto player) {
        return new Player(player.getName());
    }
}
