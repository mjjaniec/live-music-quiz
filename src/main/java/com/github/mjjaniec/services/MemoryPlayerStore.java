package com.github.mjjaniec.services;

import com.github.mjjaniec.model.Player;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MemoryPlayerStore implements PlayerStore {

    private final List<Player> players = new ArrayList<>();

    @Override
    synchronized public boolean addPlayer(String name) {
        if (players.stream().noneMatch(p -> p.name().equals(name))) {
            players.add(new Player(name));
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean hasPlayer(Player player) {
        return players.stream().anyMatch(p -> p.name().equals(player.name()));
    }

    @Override
    public List<Player> getPlayers() {
        return List.copyOf(players);
    }

    @Override
    public void removePlayer(Player player) {
        players.remove(player);
    }
}
