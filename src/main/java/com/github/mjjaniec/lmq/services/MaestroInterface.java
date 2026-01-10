package com.github.mjjaniec.lmq.services;

import com.github.mjjaniec.lmq.model.GameStage;
import com.github.mjjaniec.lmq.model.MainSet;
import com.github.mjjaniec.lmq.model.PlayOffs;
import com.github.mjjaniec.lmq.model.StageSet;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public interface MaestroInterface extends GameService {

    void initGame(MainSet set);

    boolean isGameStarted();

    void reset();

    void setStage(GameStage gameStage);

    @Nullable StageSet stageSet();

    void setCustomMessage(String customMessage);

    void clearCustomMessage();

    List<String> getFeedbacks();

    Optional<PlayOffs.PlayOff> playOffTask();

    void setPlayOffTask(PlayOffs.PlayOff playOff);

    void clearPlayOffTask();
}
