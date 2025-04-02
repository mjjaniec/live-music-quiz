package com.github.mjjaniec.lmq.stores;

import com.github.mjjaniec.lmq.model.Player;

import java.util.Map;

public interface PlayOffStore {
    void savePlayOff(Player player, int value);

    void clearPlayOffs();

    Map<String, Integer> getPlayOffs();
}
