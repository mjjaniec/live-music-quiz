package com.github.mjjaniec.stores;

import com.github.mjjaniec.model.PlayOffs;

import java.util.Optional;

public interface PlayOffTaskStore {
    void savePlayOffTask(PlayOffs.PlayOff task);
    Optional<PlayOffs.PlayOff> getPlayOffTask();
    void clearPlayOffTask();
}
