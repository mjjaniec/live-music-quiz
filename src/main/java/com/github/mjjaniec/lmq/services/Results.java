package com.github.mjjaniec.lmq.services;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record Results(int rounds, int currentRound, List<Row> rows) {
    @RequiredArgsConstructor
    public enum Award {
        FIRST("\uD83E\uDD47", "gold"),
        SECOND("\uD83E\uDD48", "silver"),
        THIRD("\uD83E\uDD49", "bronze"),
        PLAY_OFF("\uD83C\uDF52", "rust");
        public final String symbol;
        public final String style;
    }


    public record Row(String player, int ordinal, int position, Optional<Award> award, Map<Integer, Integer> rounds,
                      int playOff, int total) {
    }
}



