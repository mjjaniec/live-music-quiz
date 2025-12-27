package com.github.mjjaniec.lmq.model;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public record MainSet(List<LevelPieces> levels) {
    public record LevelPieces(RoundMode level, List<Piece> pieces) {
    }

    public record Piece(
            String artist,
            @Nullable String artistAlternative,
            String title,
            @Nullable String titleAlternative,
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

    @RequiredArgsConstructor
    public enum RoundMode {
        EVERYBODY(4, 6),
        ONION(12, 16),
        FIRST(12, 16);

        public final int artistPoints;
        public final int titlePoints;

        public boolean isEverybody() {
            return this == EVERYBODY;
        }
    }
}

