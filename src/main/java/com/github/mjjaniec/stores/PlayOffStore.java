package com.github.mjjaniec.stores;

import com.github.mjjaniec.model.Player;

import java.util.Optional;

public interface PlayOffStore {
    void savePlayOff(Player player, int value);

    Optional<Integer> getPlayOff(Player player);

    void clearPlayOffs();

}
