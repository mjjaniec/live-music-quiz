package com.github.mjjaniec.stores;

import com.github.mjjaniec.model.Player;
import com.google.common.collect.Lists;
import jakarta.transaction.Transactional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface JpaPlayerStore extends CrudRepository<Player, String>, PlayerStore {
    @Override
    @Transactional
    default boolean addPlayer(String name) {
        Player player = new Player(name);
        if (hasPlayer(player)) {
            return false;
        }
        save(player);
        return true;
    }

    @Override
    default boolean hasPlayer(Player player) {
        return existsById(player.name);
    }

    @Override
    default List<Player> getPlayers() {
        return Lists.newArrayList(findAll());
    }

    @Override
    default void removePlayer(Player player) {
        delete(player);
    }
}
