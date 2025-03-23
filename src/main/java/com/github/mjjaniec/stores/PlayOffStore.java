package com.github.mjjaniec.stores;

import com.github.mjjaniec.model.Player;

import java.util.Map;

public interface PlayOffStore {
    void savePlayOff(Player player, int value);

    void clearPlayOffs();

    Map<String, Integer> getPlayOffs();
}
