package com.github.mjjaniec.model;

import java.util.List;

public record PlayOffs(List<PlayOff> playOffs) {
    public record PlayOff(String name, int id, int value) {
    }
}
