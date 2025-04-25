package com.github.mjjaniec.lmq.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class SpreadsheetLoader {
    private static final String SetList = "set-list";
    private static final String Artists = "artists";
    private static final String Titles = "titles";
    private static final String PlayOff = "play-offs";
    private static final String DocumentId = "1PFvN5U5W9eYuTpGlPgk8bBKe-ErZUfNYG61Aw1DNUMk";
    private static final String BaseUrl = "https://docs.google.com/spreadsheets/d/" + DocumentId + "/gviz/tq?tqx=out:csv&sheet=";

    private final ObjectMapper jsonMapper = JsonMapper.builder().enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS).build();
    private final CsvSchema csvSchema = CsvSchema.emptySchema().withSkipFirstDataRow(true);
    private final CsvMapper mapper = CsvMapper.builder().enable(CsvParser.Feature.WRAP_AS_ARRAY).build();

    @SneakyThrows
    public PlayOffs loadPlayOffs() {
        try (MappingIterator<String[]> readValues =
                     mapper.readerFor(String[].class).with(csvSchema).readValues(new URI(BaseUrl + PlayOff).toURL())) {
            return new PlayOffs(readValues.readAll().stream()
                    .map(array -> new PlayOffs.PlayOff(array[1], Integer.parseInt(array[0]), Integer.parseInt(array[2])))
                    .toList());
        }
    }

    public List<String> loadTitles() {
        return loadStrings(Titles);
    }

    public List<String> loadArtists() {
        return loadStrings(Artists);
    }

    @SneakyThrows
    public MainSet loadMainSet() {
        List<MainSet.LevelPieces> result = new ArrayList<>();
        List<MainSet.Piece> pieces = new ArrayList<>();
        Optional<MainSet.Difficulty> diff = Optional.empty();
        try (MappingIterator<String[]> readValues =
                     mapper.readerFor(String[].class).with(csvSchema).readValues(new URI(BaseUrl + SetList).toURL())) {
            List<String[]> rows = readValues.readAll();
            for (String[] row : rows) {
                if (row[2].isBlank()) {
                    if (!row[6].isBlank()) {
                        List<MainSet.Piece> fPieces = pieces;
                        diff.ifPresent(d -> result.add(new MainSet.LevelPieces(d, fPieces)));
                        pieces = new ArrayList<>();
                        diff = Optional.of(read(row[6], MainSet.Difficulty.class));
                    }
                } else {
                    String artist = row[0];
                    String artistAlternative = row[1].isBlank() ? null : row[1];
                    String title = row[2];
                    Integer tempo = row[3].isBlank() ? null : Integer.parseInt(row[3]);
                    String hint = row[5];
                    Set<String> sets = readSets(row[6]);
                    MainSet.Instrument instrument = read(row[7], MainSet.Instrument.class);
                    pieces.add(new MainSet.Piece(artist, artistAlternative, title, instrument, tempo, hint, sets));
                }
            }
        }
        List<MainSet.Piece> fPieces = pieces;
        diff.ifPresent(d -> result.add(new MainSet.LevelPieces(d, fPieces)));
        return new MainSet(result);
    }

    @SneakyThrows
    private List<String> loadStrings(String tab) {
        try (MappingIterator<String[]> readValues =
                     mapper.readerFor(String[].class).with(csvSchema).readValues(new URI(BaseUrl + tab).toURL())) {
            return readValues.readAll().stream().map(array -> array[0]).toList();
        }
    }

    private Set<String> readSets(String value) {
        return Arrays.stream(value.split(",")).map(String::trim).collect(Collectors.toSet());
    }

    private <E> E read(String value, Class<E> _enum) throws JsonProcessingException {
        return jsonMapper.readValue('"' + value + '"', _enum);
    }
}
