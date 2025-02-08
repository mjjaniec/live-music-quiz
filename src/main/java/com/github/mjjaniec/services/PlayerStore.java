package com.github.mjjaniec.services;

import com.github.mjjaniec.model.Player;

import java.util.List;

public interface PlayerStore {
    boolean addPlayer(String name);
    boolean hasPlayer(Player player);
    List<Player> getPlayers();
}
