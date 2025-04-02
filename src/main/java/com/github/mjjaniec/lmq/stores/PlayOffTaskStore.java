package com.github.mjjaniec.lmq.stores;

import com.github.mjjaniec.lmq.model.PlayOffs;

import java.util.Optional;

public interface PlayOffTaskStore {
    void savePlayOffTask(PlayOffs.PlayOff task);
    Optional<PlayOffs.PlayOff> getPlayOffTask(PlayOffs playOffs);
    void clearPlayOffTask();
}
