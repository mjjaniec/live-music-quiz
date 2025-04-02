package com.github.mjjaniec.lmq.stores;

import com.github.mjjaniec.lmq.model.GameStage;
import com.github.mjjaniec.lmq.model.StageSet;

import java.util.Optional;

public interface StageStore {
    Optional<GameStage> readStage(StageSet stageSet);

    void saveStage(GameStage stage);

    void clearStage();
}
