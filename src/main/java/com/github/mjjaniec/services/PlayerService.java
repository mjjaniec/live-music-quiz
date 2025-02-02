package com.github.mjjaniec.services;

import com.github.mjjaniec.model.Player;

import java.util.List;

public interface PlayerService {
    boolean addPlayer(String name);
    List<Player> getPlayers();
}
