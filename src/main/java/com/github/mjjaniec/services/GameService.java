package com.github.mjjaniec.services;


import com.github.mjjaniec.model.GameLevel;
import com.github.mjjaniec.model.Player;
import com.github.mjjaniec.model.Quiz;

import java.util.List;

public interface GameService {
    GameLevel currentLevel();

    boolean addPlayer(String name);
    List<Player> getPlayers();

    void startListening();
    void endListening();
    void advance();

    void setQuiz(Quiz quiz);

    Quiz quiz();
}
