package com.github.mjjaniec.lmq.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SpreadsheetLoader {
    private static final String SetList = "set-list";
    private static final String Artists = "artists";
    private static final String Titles = "titles";
    private static final String PlayOff = "play-offs";
    private static final String DocumentId = "15zxKdCWWvwQrPFfB0i30tH5I09g-S8j8-ulSaKI-la0";
    private static final String BaseUrl = "https://docs.google.com/spreadsheets/d/" + DocumentId + "/gviz/tq?tqx=out:csv&sheet=";

    private final ObjectMapper jsonMapper;
    private final CsvSchema csvSchema = CsvSchema.emptySchema().withSkipFirstDataRow(true);
    private final CsvMapper mapper = CsvMapper.builder().enable(CsvParser.Feature.WRAP_AS_ARRAY).build();

    @SneakyThrows
    public PlayOffs loadPlayOffs() {
        try (MappingIterator<String[]> readValues =
                     mapper.readerFor(String[].class).with(csvSchema).readValues(new URI(BaseUrl + PlayOff).toURL().openStream())) {
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
        Optional<MainSet.RoundMode> mode = Optional.empty();
        try (MappingIterator<String[]> readValues =
                     mapper.readerFor(String[].class).with(csvSchema).readValues(new URI(BaseUrl + SetList).toURL().openStream())) {
            List<String[]> rows = readValues.readAll();
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
        }
        List<MainSet.Piece> fPieces = pieces;
        mode.ifPresent(d -> result.add(new MainSet.LevelPieces(d, fPieces)));
        return validateMainSet(new MainSet(result));
    }

    private MainSet validateMainSet(MainSet set) {
        Set<String> artists = new HashSet<>(loadArtists());
        Set<String> titles = new HashSet<>(loadTitles());
        var invalids = set.levels().stream().flatMap(l -> l.pieces().stream()).filter(piece ->
                !(piece.artist().equals(Constants.UNKNOWN) || artists.contains(piece.artist())) ||
                !(piece.artistAlternative() == null || artists.contains(piece.artistAlternative())) ||
                !titles.contains(piece.title()) ||
                !(piece.titleAlternative() == null || titles.contains(piece.titleAlternative()))
        ).map(MainSet.Piece::toString).toList();
        if (!invalids.isEmpty()) {
            throw new RuntimeException("The following pieces do not match with hints\n" + String.join("\n", invalids));
        }
        return set;
    }

    @SneakyThrows
    private List<String> loadStrings(String tab) {
        try (MappingIterator<String[]> readValues =
                     mapper.readerFor(String[].class).with(csvSchema).readValues(new URI(BaseUrl + tab).toURL().openStream())) {
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
