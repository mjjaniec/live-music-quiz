package com.github.mjjaniec.lmq.model;

import java.util.List;

public record PlayOffs(List<PlayOff> playOffs) {
    public record PlayOff(String name, int id, int value) {
    }
}
