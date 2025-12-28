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

    @Nullable GameStage stage();

    Optional<GameStage.RoundInit> roundInitStage();

    Optional<GameStage.RoundPiece> pieceStage();

    Optional<GameStage.WrapUp> wrapUpStage();

    Optional<GameStage.PlayOff> playOffStage();

    Optional<GameStage.RoundSummary> roundSummaryStage();

    List<Player> getPlayers();

    List<Player> getSlackers();

    int getCurrentPlayerPoints(Player player);

    Optional<String> customMessage();

    Optional<Answer> getCurrentAnswer(Player player);

    void reportResult(Player player, boolean artist, boolean title, @Nullable String actualArtist, @Nullable String actualTitle);

    void savePlayOff(Player player, int value);

    @Nullable StageSet stageSet();

    Results results();

    void saveFeedback(String value);

    void raise(Player player);
}
