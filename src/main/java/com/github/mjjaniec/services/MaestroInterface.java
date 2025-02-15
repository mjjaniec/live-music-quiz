package com.github.mjjaniec.services;

import com.github.mjjaniec.model.GameStage;
import com.github.mjjaniec.model.MainSet;

import java.util.List;

public interface MaestroInterface extends GameService {

    void initGame(MainSet set);

    void reset();

    void setStage(GameStage<?,?> gameStage);

    List<GameStage<?,?>> allStages();
}
