package com.github.mjjaniec.services;


import com.github.mjjaniec.model.GameStage;
import com.github.mjjaniec.model.MainSet;
import com.github.mjjaniec.model.Player;

import java.util.List;
import java.util.Optional;

public interface GameService {

    boolean hasPlayer(Player player);

    boolean addPlayer(String name);

    void removePlayer(Player player);

    GameStage stage();

    List<Player> getPlayers();

    MainSet quiz();

    Optional<String> customMessage();

    void reportResult(Optional<Player> player, boolean artist, boolean title, boolean bonus);
}
