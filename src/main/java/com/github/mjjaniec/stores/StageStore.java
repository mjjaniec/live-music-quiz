package com.github.mjjaniec.stores;

import com.github.mjjaniec.model.GameStage;
import com.github.mjjaniec.model.StageSet;

import java.util.Optional;

public interface StageStore {
    Optional<GameStage> readStage(StageSet stageSet);

    void saveStage(GameStage stage);

    void clearStage();
}
