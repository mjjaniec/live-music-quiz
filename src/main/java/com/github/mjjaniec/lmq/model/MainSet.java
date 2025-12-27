package com.github.mjjaniec.lmq.model;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public record MainSet(List<LevelPieces> levels) {
    @RequiredArgsConstructor
    public enum Difficulty {
        Easiest(RoundMode.EVERYBODY, new RoundPoints(2, 3)),
        Easy(RoundMode.EVERYBODY, new RoundPoints(3, 4)),
        Moderate(RoundMode.FIRST, new RoundPoints(5, 8)),
        Moderate_All(RoundMode.EVERYBODY, new RoundPoints(4, 6)),
        Hard(RoundMode.FIRST, new RoundPoints(10, 15)),
        Harder(RoundMode.FIRST, new RoundPoints(15, 20)),
        Impossible(RoundMode.FIRST, new RoundPoints(20, 30));

        public final RoundMode mode;
        public final RoundPoints points;
    }

    public record LevelPieces(Difficulty level, List<Piece> pieces) {
    }

    public record Piece(
            String artist,
            @Nullable String artistAlternative,
            String title,
            @Nullable Integer tempo,
            @Nullable String hint,
            Set<String> sets
    ) {
    }


    public Set<String> sets() {
        return levels.stream().flatMap(level -> level.pieces.stream()).flatMap(piece -> piece.sets().stream()).collect(Collectors.toSet());
    }

    public MainSet asSet(String set) {
        return new MainSet(
                levels.stream()
                        .map(level -> new LevelPieces(level.level, level.pieces.stream().filter(piece -> piece.sets.contains(set)).toList()))
                        .filter(level -> !level.pieces.isEmpty())
                        .toList()
        );
    }

    public enum RoundMode {
        EVERYBODY,
        FIRST
    }

    public record RoundPoints(int artist, int title) {
    }
}

