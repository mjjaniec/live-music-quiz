package com.github.mjjaniec.services;


import com.github.mjjaniec.model.GameStage;
import com.github.mjjaniec.model.MainSet;
import com.github.mjjaniec.model.Player;

import java.util.List;

public interface GameService {

    boolean hasPlayer(Player player);

    boolean addPlayer(String name);

    void removePlayer(Player player);

    GameStage<?,?> stage();

    List<Player> getPlayers();

    MainSet quiz();

}
