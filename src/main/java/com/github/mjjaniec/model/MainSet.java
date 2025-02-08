package com.github.mjjaniec.model;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.helger.commons.io.resource.ClassPathResource;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record MainSet(List<LevelPieces> levels) {
    public enum Instrument {
        Bass, Piano, Guitar, Xylophone
    }

    @RequiredArgsConstructor
    public enum Difficulty {
        Easiest(RoundMode.EVERYBODY, new RoundPoints(2, 3)),
        Easy(RoundMode.EVERYBODY, new RoundPoints(3, 4)),
        Moderate(RoundMode.FIRST, new RoundPoints(5, 8)),
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

    public static final MainSet TheSet;

    static {
        ClassPathResource resource = new ClassPathResource("pieces.yml");
        YAMLMapper yamlMapper = YAMLMapper.builder().enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS).build();
        try {
            TheSet = yamlMapper.reader().readValue(resource.getInputStream(), MainSet.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public enum RoundMode {
        EVERYBODY,
        FIRST;
    }

    public record RoundPoints(int artist, int title) {
    }
}

