package com.github.mjjaniec.services;


import com.github.mjjaniec.model.GameStage;
import com.github.mjjaniec.model.Player;
import com.github.mjjaniec.model.StageSet;

import java.util.List;
import java.util.Optional;

public interface GameService {

    boolean hasPlayer(Player player);

    boolean addPlayer(String name);

    void removePlayer(Player player);

    GameStage stage();

    List<Player> getPlayers();

    List<Player> getSlackers();

    int getCurrentPlayerPoints(Player player);

    Optional<String> customMessage();

    void reportResult(Player player, boolean artist, boolean title, boolean bonus);

    StageSet stageSet();
}
