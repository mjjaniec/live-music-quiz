package com.github.mjjaniec.lmq.stores;

import com.github.mjjaniec.lmq.model.Player;

import java.util.List;

public interface PlayerStore {
    boolean addPlayer(String name);

    boolean hasPlayer(Player player);

    List<Player> getPlayers();

    void removePlayer(Player player);

    void clearPlayers();
}
