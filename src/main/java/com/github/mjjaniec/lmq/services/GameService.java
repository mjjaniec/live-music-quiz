package com.github.mjjaniec.lmq.services;


import com.github.mjjaniec.lmq.model.Answer;
import com.github.mjjaniec.lmq.model.GameStage;
import com.github.mjjaniec.lmq.model.Player;
import com.github.mjjaniec.lmq.model.StageSet;

import org.jspecify.annotations.Nullable;
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

    Optional<Answer> getCurrentAnswer(Player player);

    void reportResult(Player player, boolean artist, boolean title, int bonus, @Nullable String actualArtist, @Nullable String actualTitle);

    void savePlayOff(Player player, int value);

    StageSet stageSet();

    Results results();

    void saveFeedback(String value);

    void raise(Player player);
}
