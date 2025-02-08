package com.github.mjjaniec.services;


import com.github.mjjaniec.model.MainSet;
import com.github.mjjaniec.model.Player;

import java.util.List;

public interface GameService {

    boolean hasPlayer(Player player);

    boolean addPlayer(String name);

    List<Player> getPlayers();

    void startListening();

    void endListening();

    void advance();

    MainSet quiz();

    void setSet(MainSet set);

    void reset();
}
