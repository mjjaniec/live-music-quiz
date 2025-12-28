package com.github.mjjaniec.lmq.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MainSetParserTest {

    private MainSetParser parser;
    private Set<String> artists;
    private Set<String> titles;

    @BeforeEach
    void setUp() {
        parser = new MainSetParser(new ObjectMapper());
        artists = Set.of("Artist 1", "Artist 2");
        titles = Set.of("Title 1", "Title 2");
    }

    @Test
    void testParseValidRows() {
        List<String[]> rows = new ArrayList<>();
        // Round header
        rows.add(new String[]{"", "", "", "", "", "", "EVERYBODY"});
        // Piece 1
        rows.add(new String[]{"Artist 1", "Artist 2", "Title 1", "Title 2", "120", "Hint 1", "SetA, SetB"});
        // Piece 2
        rows.add(new String[]{"Artist 2", "", "Title 2", "", "", "", "SetA"});

        MainSet mainSet = parser.parse(rows, artists, titles);

        assertNotNull(mainSet);
        assertEquals(1, mainSet.levels().size());
        MainSet.LevelPieces level = mainSet.levels().get(0);
        assertEquals(MainSet.RoundMode.EVERYBODY, level.level());
        assertEquals(2, level.pieces().size());

        MainSet.Piece p1 = level.pieces().get(0);
        assertEquals("Artist 1", p1.artist());
        assertEquals("Artist 2", p1.artistAlternative());
        assertEquals("Title 1", p1.title());
        assertEquals("Title 2", p1.titleAlternative());
        assertEquals(120, p1.tempo());
        assertEquals("Hint 1", p1.hint());
        assertEquals(Set.of("SetA", "SetB"), p1.sets());

        MainSet.Piece p2 = level.pieces().get(1);
        assertEquals("Artist 2", p2.artist());
        assertNull(p2.artistAlternative());
        assertEquals("Title 2", p2.title());
        assertNull(p2.titleAlternative());
        assertNull(p2.tempo());
        assertEquals("", p2.hint());
        assertEquals(Set.of("SetA"), p2.sets());
    }

    @Test
    void testParseMultipleLevels() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"", "", "", "", "", "", "EVERYBODY"});
        rows.add(new String[]{"Artist 1", "", "Title 1", "", "", "", "SetA"});
        rows.add(new String[]{"", "", "", "", "", "", "ONION"});
        rows.add(new String[]{"Artist 2", "", "Title 2", "", "", "", "SetA"});

        MainSet mainSet = parser.parse(rows, artists, titles);

        assertEquals(2, mainSet.levels().size());
        assertEquals(MainSet.RoundMode.EVERYBODY, mainSet.levels().get(0).level());
        assertEquals(MainSet.RoundMode.ONION, mainSet.levels().get(1).level());
    }

    @Test
    void testValidationFailsForUnknownArtist() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"", "", "", "", "", "", "EVERYBODY"});
        rows.add(new String[]{"Invalid Artist", "", "Title 1", "", "", "", "SetA"});

        RuntimeException exception = assertThrows(RuntimeException.class, () -> parser.parse(rows, artists, titles));
        assertTrue(exception.getMessage().contains("artist: Invalid Artist"));
    }

    @Test
    void testValidationFailsForUnknownTitle() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"", "", "", "", "", "", "EVERYBODY"});
        rows.add(new String[]{"Artist 1", "", "Invalid Title", "", "", "", "SetA"});

        RuntimeException exception = assertThrows(RuntimeException.class, () -> parser.parse(rows, artists, titles));
        assertTrue(exception.getMessage().contains("title: Invalid Title"));
    }

    @Test
    void testValidationPassesForUnknownConstant() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"", "", "", "", "", "", "EVERYBODY"});
        rows.add(new String[]{Constants.UNKNOWN, "", "Title 1", "", "", "", "SetA"});

        assertDoesNotThrow(() -> parser.parse(rows, artists, titles));
    }
}
