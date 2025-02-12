package com.github.mjjaniec.services;

import com.github.mjjaniec.model.GameStage;
import com.github.mjjaniec.model.MainSet;

public interface MaestroInterface extends GameService {

    void initGame(MainSet set);

    void reset();

    void setStage(GameStage<?,?> gameStage);
}
