package com.github.mjjaniec.lmq.services;

import com.github.mjjaniec.lmq.model.*;
import com.github.mjjaniec.lmq.stores.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PointsCounterTest {

    private PointsCounter pointsCounter;

    private final MainSet.Piece piece = new MainSet.Piece("Artist", null, "Title", null, null, null, new HashSet<>());
    private final GameStage.RoundNumber roundNumber = new GameStage.RoundNumber(1, 1);
    private final GameStage.RoundSummary dummySummary = new GameStage.RoundSummary(roundNumber);
    private final GameStage.RoundPiece roundPiece = new GameStage.RoundPiece(1, new GameStage.PieceNumber(1, 1), piece, List.of(GameStage.PieceStage.LISTEN));

    @BeforeEach
    void setUp() {
        pointsCounter = new PointsCounter();
    }
    

    @Test
    void testResultsCalculation() {
        Player p1 = new Player("P1");
        Player p2 = new Player("P2");

        MainSet.LevelPieces level = new MainSet.LevelPieces(MainSet.RoundMode.EVERYBODY, List.of(piece));
        MainSet quiz = new MainSet(List.of(level));
        StageSet stageSet = new StageSet(quiz);
        GameStage.RoundSummary summary = new GameStage.RoundSummary(new GameStage.RoundNumber(1, 1));

        Answer a1 = new Answer(true, true, 10, "P1", 1, 1, "Artist", "Title");
        Answer a2 = new Answer(true, false, 4, "P2", 1, 1, "Artist", "Title");

        Results results = pointsCounter.results(summary, stageSet,
                List.of(p1, p2), Stream.of(a1, a2), Optional.empty(), Map.of());

        assertEquals(1, results.rounds());
        assertEquals(1, results.currentRound());
        assertEquals(2, results.rows().size());
        
        Results.Row r1 = results.rows().stream().filter(r -> r.player().equals("P1")).findFirst().orElseThrow();
        Results.Row r2 = results.rows().stream().filter(r -> r.player().equals("P2")).findFirst().orElseThrow();

        assertEquals(10, r1.total());
        assertEquals(4, r2.total());
        assertEquals(1, r1.position());
        assertEquals(2, r2.position());
    }

    @Test
    void testPointsCalculation_Everybody() {
        var roundInit = new GameStage.RoundInit(roundNumber, MainSet.RoundMode.EVERYBODY, List.of(roundPiece), dummySummary);
        // Everybody mode: artist 4, title 6
        assertEquals(10, pointsCounter.points(true, true, roundInit, roundPiece));
        assertEquals(4, pointsCounter.points(true, false, roundInit, roundPiece));
        assertEquals(6, pointsCounter.points(false, true, roundInit, roundPiece));
        assertEquals(0, pointsCounter.points(false, false, roundInit, roundPiece));

        roundPiece.setBonus(true);

        // Everybody mode: artist 4, title 6
        assertEquals(20, pointsCounter.points(true, true, roundInit, roundPiece));
        assertEquals(8, pointsCounter.points(true, false, roundInit, roundPiece));
        assertEquals(12, pointsCounter.points(false, true, roundInit, roundPiece));
        assertEquals(0, pointsCounter.points(false, false, roundInit, roundPiece));
    }

    @Test
    void testPointsCalculation_Onion() {
         // Onion mode: artist 2, title 3. Multipliers: 0->4, 1-2->3, 3-5->2, 6+ -> 1
        var roundInit = new GameStage.RoundInit(roundNumber, MainSet.RoundMode.ONION, List.of(roundPiece), dummySummary);
        assertEquals(2*4 + 3*4, pointsCounter.points(true, true, roundInit, roundPiece));
        assertEquals(3*4, pointsCounter.points(false, true, roundInit, roundPiece));
        assertEquals(2*4, pointsCounter.points(true, false, roundInit, roundPiece));

        roundPiece.incrementArtistAnswered(); // artistAnswered = 1
        assertEquals(2*3 + 3*4, pointsCounter.points(true, true, roundInit, roundPiece));

        roundPiece.incrementArtistAnswered(); // artistAnswered = 2
        assertEquals(2*3 + 3*4, pointsCounter.points(true, true, roundInit, roundPiece));

        roundPiece.incrementArtistAnswered(); // artistAnswered = 3
        assertEquals(2*2 + 3*4, pointsCounter.points(true, true, roundInit, roundPiece));

        roundPiece.incrementTitleAnswered(); // artistAnswered = 3
        assertEquals(2*2 + 3*3, pointsCounter.points(true, true, roundInit, roundPiece));


    }

    @Test
    void testPointsCalculation_First() {
        // First mode: artist 12, title 16. Multiplier: 1 + failedResponders
        var roundInit = new GameStage.RoundInit(roundNumber, MainSet.RoundMode.FIRST, List.of(roundPiece), dummySummary);
        assertEquals(12 + 16, pointsCounter.points(true, true, roundInit, roundPiece));
        roundPiece.addFailedResponder("Other");
        assertEquals((12 + 16) * 2, pointsCounter.points(true, true, roundInit, roundPiece));
    }

    @Test
    void testComplexResultsCalculation() {
        // 7 players
        List<Player> players = Stream.of("P1", "P2", "P3", "P4", "P5", "P6", "P7")
                .map(Player::new).toList();

        // 3 rounds, each with 3 pieces, different modes
        MainSet.Piece piece = new MainSet.Piece("Artist", null, "Title", null, null, null, new HashSet<>());
        List<MainSet.Piece> threePieces = List.of(piece, piece, piece);

        MainSet quiz = new MainSet(List.of(
                new MainSet.LevelPieces(MainSet.RoundMode.EVERYBODY, threePieces),
                new MainSet.LevelPieces(MainSet.RoundMode.ONION, threePieces),
                new MainSet.LevelPieces(MainSet.RoundMode.FIRST, threePieces)
        ));

        StageSet stageSet = new StageSet(quiz);
        // Stage: end of round 3
        GameStage.RoundSummary summary = new GameStage.RoundSummary(new GameStage.RoundNumber(3, 3));
        

        List<Answer> answers = List.of(
                // P1: 10*3 + 20*3 + (28 + 40 + 40) = 30 + 60 + 108 = 198
                new Answer(true, true, 10, "P1", 1, 1, "A", "T"),
                new Answer(true, true, 10, "P1", 1, 2, "A", "T"),
                new Answer(true, true, 10, "P1", 1, 3, "A", "T"),
                new Answer(true, true, 20, "P1", 2, 1, "A", "T"),
                new Answer(true, true, 20, "P1", 2, 2, "A", "T"),
                new Answer(true, true, 20, "P1", 2, 3, "A", "T"),
                new Answer(true, true, 28, "P1", 3, 1, "A", "T"),
                new Answer(true, true, 40, "P1", 3, 2, "A", "T"),
                new Answer(true, true, 40, "P1", 3, 3, "A", "T"),

                // P2: 4*3 + (8 + 12 + 8) + (12 + 24 + 8) = 12 + 28 + 44 = 84
                new Answer(true, false, 4, "P2", 1, 1, "A", "T"),
                new Answer(true, false, 4, "P2", 1, 2, "A", "T"),
                new Answer(true, false, 4, "P2", 1, 3, "A", "T"),
                new Answer(true, false, 8, "P2", 2, 1, "A", "T"),
                new Answer(true, false, 12, "P2", 2, 2, "A", "T"),
                new Answer(true, false, 8, "P2", 2, 3, "A", "T"),
                new Answer(true, false, 12, "P2", 3, 1, "A", "T"),
                new Answer(true, false, 24, "P2", 3, 2, "A", "T"),
                new Answer(true, false, 8, "P2", 3, 3, "A", "T"),

                // P3: 6*3 + 12*3 + 20*3 = 18 + 36 + 60 = 114
                new Answer(false, true, 6, "P3", 1, 1, "A", "T"),
                new Answer(false, true, 6, "P3", 1, 2, "A", "T"),
                new Answer(false, true, 6, "P3", 1, 3, "A", "T"),
                new Answer(false, true, 12, "P3", 2, 1, "A", "T"),
                new Answer(false, true, 12, "P3", 2, 2, "A", "T"),
                new Answer(false, true, 12, "P3", 2, 3, "A", "T"),
                new Answer(false, true, 20, "P3", 3, 1, "A", "T"),
                new Answer(false, true, 20, "P3", 3, 2, "A", "T"),
                new Answer(false, true, 20, "P3", 3, 3, "A", "T"),

                // P4
                new Answer(true, true, 10, "P4", 1, 1, "A", "T"),
                new Answer(true, true, 10, "P4", 1, 2, "A", "T"),
                new Answer(true, true, 10, "P4", 1, 3, "A", "T"),

                // P5
                new Answer(true, true, 28, "P5", 2, 1, "A", "T"),
                new Answer(true, true, 28, "P5", 2, 2, "A", "T"),
                new Answer(true, true, 28, "P5", 2, 3, "A", "T"),

                // P6
                new Answer(true, true, 28, "P6", 3, 1, "A", "T"),
                new Answer(true, true, 28, "P6", 3, 2, "A", "T"),
                new Answer(true, true, 28, "P6", 3, 3, "A", "T")
        );

        Results results = pointsCounter.results(summary, stageSet, players, answers.stream(), Optional.empty(), Map.of());

        assertEquals(3, results.rounds());
        assertEquals(3, results.currentRound());
        assertEquals(7, results.rows().size());

        Map<String, Results.Row> rowsByPlayer = results.rows().stream()
                .collect(Collectors.toMap(Results.Row::player, r -> r));

        assertEquals(198, rowsByPlayer.get("P1").total());
        assertEquals(114, rowsByPlayer.get("P3").total());
        assertEquals(84, rowsByPlayer.get("P2").total());
        assertEquals(84, rowsByPlayer.get("P5").total());
        assertEquals(84, rowsByPlayer.get("P6").total());
        assertEquals(30, rowsByPlayer.get("P4").total());
        assertEquals(0, rowsByPlayer.get("P7").total());

        assertEquals(1, rowsByPlayer.get("P1").position());
        assertEquals(2, rowsByPlayer.get("P3").position());
        assertEquals(3, rowsByPlayer.get("P2").position());
        assertEquals(3, rowsByPlayer.get("P5").position());
        assertEquals(3, rowsByPlayer.get("P6").position());
        assertEquals(6, rowsByPlayer.get("P4").position());
        assertEquals(7, rowsByPlayer.get("P7").position());
    }
}
