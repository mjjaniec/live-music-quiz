package com.github.mjjaniec.services;

import com.github.mjjaniec.model.GameStage;
import com.github.mjjaniec.model.MainSet;
import com.github.mjjaniec.model.StageSet;

import java.util.List;

public interface MaestroInterface extends GameService {

    void initGame(MainSet set);

    boolean isGameStarted();

    void reset();

    void setStage(GameStage gameStage);

    StageSet stageSet();

    void setCustomMessage(String customMessage);

    void clearCustomMessage();

    List<String> getFeedbacks();

    GameStage.Display minimalDisplay();
}
