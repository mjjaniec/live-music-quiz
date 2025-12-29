package com.github.mjjaniec.lmq.model;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.HashSet;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SpreadsheetLoader {
    private static final String SetList = "set-list";
    private static final String Artists = "artists";
    private static final String Titles = "titles";
    private static final String PlayOff = "play-offs";

    private final MainSetParser mainSetParser;
    private final CsvSchema csvSchema = CsvSchema.emptySchema().withSkipFirstDataRow(true);
    private final CsvMapper mapper = CsvMapper.builder().enable(CsvParser.Feature.WRAP_AS_ARRAY).build();
    private @Value("https://docs.google.com/spreadsheets/d/${application.csm-spreadsheet-id}/gviz/tq?tqx=out:csv&sheet=") String BaseUrl;

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
        try (MappingIterator<String[]> readValues =
                     mapper.readerFor(String[].class).with(csvSchema).readValues(new URI(BaseUrl + SetList).toURL().openStream())) {
            return mainSetParser.parse(readValues.readAll(), new HashSet<>(loadArtists()), new HashSet<>(loadTitles()));
        }
    }

    @SneakyThrows
    private List<String> loadStrings(String tab) {
        try (MappingIterator<String[]> readValues =
                     mapper.readerFor(String[].class).with(csvSchema).readValues(new URI(BaseUrl + tab).toURL().openStream())) {
            return readValues.readAll().stream().map(array -> array[0]).toList();
        }
    }

}
