package com.github.mjjaniec.services;

import com.github.mjjaniec.model.GameStage;
import com.github.mjjaniec.model.MainSet;
import com.github.mjjaniec.model.StageSet;

public interface MaestroInterface extends GameService {

    void initGame(MainSet set);

    void reset();

    void setStage(GameStage gameStage);

    StageSet stageSet();

    void setCustomMessage(String customMessage);

    void clearCustomMessage();
}
