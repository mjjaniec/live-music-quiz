package com.github.mjjaniec.lmq.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class MainSetParser {
    private final ObjectMapper jsonMapper;

    public MainSet parse(List<String[]> rows, Set<String> artists, Set<String> titles) {
        List<MainSet.LevelPieces> result = new ArrayList<>();
        List<MainSet.Piece> pieces = new ArrayList<>();
        Optional<MainSet.RoundMode> mode = Optional.empty();

        for (String[] row : rows) {
            if (row[2].isBlank()) {
                if (!row[6].isBlank()) {
                    List<MainSet.Piece> fPieces = pieces;
                    mode.ifPresent(d -> result.add(new MainSet.LevelPieces(d, fPieces)));
                    pieces = new ArrayList<>();
                    mode = Optional.of(read(row[6], MainSet.RoundMode.class));
                }
            } else {
                String artist = row[0];
                String artistAlternative = row[1].isBlank() ? null : row[1];
                String title = row[2];
                String titleAlternative = row[3].isBlank() ? null : row[3];
                Integer tempo = row[4].isBlank() ? null : Integer.parseInt(row[4]);
                String hint = row[5];
                Set<String> sets = readSets(row[6]);
                pieces.add(new MainSet.Piece(artist, artistAlternative, title, titleAlternative, tempo, hint, sets));
            }
        }
        List<MainSet.Piece> fPieces = pieces;
        mode.ifPresent(d -> result.add(new MainSet.LevelPieces(d, fPieces)));

        MainSet mainSet = new MainSet(result);
        validateMainSet(mainSet, artists, titles);
        return mainSet;
    }

    private void validateMainSet(MainSet set, Set<String> artists, Set<String> titles) {
        var invalids = set.levels().stream().flatMap(l -> l.pieces().stream()).flatMap(piece -> {
            List<String> invalidFields = new ArrayList<>();
            if (!(piece.artist().equals(Constants.UNKNOWN) || artists.contains(piece.artist()))) {
                invalidFields.add("artist: " + piece.artist());
            }
            if (!(piece.artistAlternative() == null || artists.contains(piece.artistAlternative()))) {
                invalidFields.add("artistAlternative: " + piece.artistAlternative());
            }
            if (!titles.contains(piece.title())) {
                invalidFields.add("title: " + piece.title());
            }
            if (!(piece.titleAlternative() == null || titles.contains(piece.titleAlternative()))) {
                invalidFields.add("titleAlternative: " + piece.titleAlternative());
            }

            if (invalidFields.isEmpty()) {
                return Stream.empty();
            } else {
                return Stream.of(piece + " -> " + invalidFields);
            }
        }).toList();

        if (!invalids.isEmpty()) {
            throw new RuntimeException("The following pieces do not match with hints\n" + String.join("\n", invalids));
        }
    }

    private Set<String> readSets(String value) {
        return Arrays.stream(value.split(",")).map(String::trim).collect(Collectors.toSet());
    }

    private <E> E read(String value, Class<E> _enum) {
        try {
            return jsonMapper.readValue('"' + value + '"', _enum);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
