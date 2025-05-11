package com.github.mjjaniec.lmq.model;

import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public record MainSet(List<LevelPieces> levels) {
    @RequiredArgsConstructor
    public enum Instrument {
        Bass("\uD834\uDD22"), Piano("\uD83C\uDFB9"), Guitar("\uD83C\uDFB8"), Xylophone("\uD83C\uDFBC");
        public final String icon;
    }

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
        public LevelPieces shuffle() {
            List<Instrument> instruments = new ArrayList<>();
            pieces.stream().map(p -> p.instrument).distinct().forEach(instruments::add);
            Collections.shuffle(instruments);
            Map<Instrument, List<Piece>> byInstrument = pieces.stream().collect(Collectors.groupingBy(p -> p.instrument));
            List<Piece> shuffled = instruments.stream().flatMap(instrument -> {
                List<Piece> shufflable = new ArrayList<>(byInstrument.getOrDefault(instrument, new ArrayList<>()));
                Collections.shuffle(shufflable);
                return shufflable.stream();
            }).toList();
            return new LevelPieces(level, shuffled);
        }
    }

    public record Piece(
            String artist,
            @Nullable String artistAlternative,
            String title,
            Instrument instrument,
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

    public MainSet shuffle() {
        return new MainSet(levels.stream().map(LevelPieces::shuffle).toList());
    }
}

