package com.github.mjjaniec.model;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record Results(int rounds, int currentRound, List<Row> rows) {
    @RequiredArgsConstructor
    public enum Award {
        FIRST("\uD83E\uDDC5", "gold"),
        SECOND("\uD83E\uDDC4", "silver"),
        THIRD("\uD83E\uDD54", "bronze"),
        PLAY_OFF("\uD83C\uDF36", "rust");
        public final String symbol;
        public final String style;
    }


    public record Row(String player, int ordinal, int position, Optional<Award> award, Map<Integer, Integer> rounds,
                      int playOff, int total) {
    }
}



