package com.github.mjjaniec.lmq.stores;

import com.github.mjjaniec.lmq.model.Player;
import com.google.common.collect.ImmutableMap;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public interface JpaPlayOffStore extends CrudRepository<PlayOffDto, String>, PlayOffStore {

    @Override
    default void clearPlayOffs() {
        deleteAll();
    }

    @Override
    default Map<String, Integer> getPlayOffs() {
        Map<String, Integer> result = new HashMap<>();
        findAll().iterator().forEachRemaining(dto -> result.put(dto.getPlayer(), dto.getAnswer()));
        return ImmutableMap.copyOf(result);
    }

    @Override
    default void savePlayOff(Player player, int answer) {
        PlayOffDto dto = new PlayOffDto();
        dto.setPlayer(player.name());
        dto.setAnswer(answer);
        save(dto);
    }

}
