package com.github.mjjaniec.tools;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.github.mjjaniec.lmq.model.MainSet;
import lombok.SneakyThrows;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.net.URI;
import java.util.*;

public class Hot100Parser {
    private static final String hot100Url = "https://raw.githubusercontent.com/utdata/rwd-billboard-data/refs/heads/main/data-out/hot-100-current.csv";
    @SneakyThrows
    public static void main(String [] args) {

        Set<String> titles = new TreeSet<>();
        Set<String> artists = new TreeSet<>();

        CsvMapper mapper = new CsvMapper();
        CsvSchema bootstrapSchema = CsvSchema.emptySchema().withSkipFirstDataRow(true);
        mapper.enable(CsvParser.Feature.WRAP_AS_ARRAY);

        try (MappingIterator<String[]> readValues =
                     mapper.readerFor(String[].class).with(bootstrapSchema).readValues(new URI(hot100Url).toURL().openStream())) {

            List<String[]> rows = readValues.readAll();
            for (String[] row : rows) {
                if (Math.random() < 0.05 )
                    titles.add(row[2]);
                if (Math.random() < 0.2) {
                    artists.add(row[3]);
                }
            }
        }

        artists.removeIf(a -> a.toLowerCase().contains("featuring") || a.contains(" & ") || a.contains("/"));



        try (BufferedWriter writer = new BufferedWriter(new FileWriter("artists"))) {
            for (var a : artists) {
                writer.write(a + "\n");
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("titles"))) {
            for (var t : titles) {
                writer.write(t + "\n");
            }
        }

        System.out.println(titles.size());
        System.out.println(artists.size());
    }
}
