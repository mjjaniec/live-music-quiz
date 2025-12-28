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

    @BeforeEach
    void setUp() {
        pointsCounter = new PointsCounter();
    }

    @Test
    void testPiecePoints() {
        MainSet.Piece piece = new MainSet.Piece("Artist", null, "Title", null, null, null, new HashSet<>());
        GameStage.RoundPiece roundPiece = new GameStage.RoundPiece(1, new GameStage.PieceNumber(1, 1), piece, List.of(GameStage.PieceStage.LISTEN));

        MainSet.LevelPieces level = new MainSet.LevelPieces(MainSet.RoundMode.EVERYBODY, List.of(piece));
        MainSet quiz = new MainSet(List.of(level));
        StageSet stageSet = new StageSet(quiz);

        Answer answer = new Answer(true, true, 1, "Player1", 1, 1, "Artist", "Title");

        int points = pointsCounter.piecePoints(roundPiece, stageSet, Optional.of(answer));

        // EVERYBODY mode: artistPoints=4, titlePoints=6. Both true, bonus=1 => 4+6=10
        assertEquals(10, points);
    }

    @Test
    void testResultsCalculation() {
        Player p1 = new Player("P1");
        Player p2 = new Player("P2");

        MainSet.Piece piece = new MainSet.Piece("Artist", null, "Title", null, null, null, new HashSet<>());
        MainSet.LevelPieces level = new MainSet.LevelPieces(MainSet.RoundMode.EVERYBODY, List.of(piece));
        MainSet quiz = new MainSet(List.of(level));
        StageSet stageSet = new StageSet(quiz);
        GameStage.RoundSummary summary = new GameStage.RoundSummary(new GameStage.RoundNumber(1, 1));

        Answer a1 = new Answer(true, true, 1, "P1", 1, 1, "Artist", "Title");
        Answer a2 = new Answer(true, false, 1, "P2", 1, 1, "Artist", "Title");

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

        // Generate answers
        // EVERYBODY (R1): A:4, T:6
        // ONION (R2): A:12, T:16
        // FIRST (R3): A:12, T:16

        // P1: all correct everywhere. 
        // R1: 3 * (4+6) = 30
        // R2: 3 * (12+16) = 84
        // R3: 3 * (12+16) = 84
        // Total: 30 + 84 + 84 = 198

        // P2: only artist correct everywhere.
        // R1: 3 * 4 = 12
        // R2: 3 * 12 = 36
        // R3: 3 * 12 = 36
        // Total: 12 + 36 + 36 = 84

        // P3: only title correct everywhere.
        // R1: 3 * 6 = 18
        // R2: 3 * 16 = 48
        // R3: 3 * 16 = 48
        // Total: 18 + 48 + 48 = 114

        // P4: R1 correct, others wrong.
        // Total: 30

        // P5: R2 correct, others wrong.
        // Total: 84 (same as P2)

        // P6: R3 correct, others wrong.
        // Total: 84 (same as P2, P5)

        // P7: nothing correct.
        // Total: 0

        List<Answer> answers = List.of(
                // P1
                new Answer(true, true, 1, "P1", 1, 1, "A", "T"),
                new Answer(true, true, 1, "P1", 1, 2, "A", "T"),
                new Answer(true, true, 2, "P1", 1, 3, "A", "T"),
                new Answer(true, true, 1, "P1", 2, 1, "A", "T"),
                new Answer(true, true, 1, "P1", 2, 2, "A", "T"),
                new Answer(true, true, 1, "P1", 2, 3, "A", "T"),
                new Answer(true, true, 1, "P1", 3, 1, "A", "T"),
                new Answer(true, true, 1, "P1", 3, 2, "A", "T"),
                new Answer(true, true, 1, "P1", 3, 3, "A", "T"),

                // P2
                new Answer(true, false, 1, "P2", 1, 1, "A", "T"),
                new Answer(true, false, 1, "P2", 1, 2, "A", "T"),
                new Answer(true, false, 1, "P2", 1, 3, "A", "T"),
                new Answer(true, false, 1, "P2", 2, 1, "A", "T"),
                new Answer(true, false, 2, "P2", 2, 2, "A", "T"),
                new Answer(true, false, 1, "P2", 2, 3, "A", "T"),
                new Answer(true, false, 1, "P2", 3, 1, "A", "T"),
                new Answer(true, false, 2, "P2", 3, 2, "A", "T"),
                new Answer(true, false, 1, "P2", 3, 3, "A", "T"),

                // P3
                new Answer(false, true, 1, "P3", 1, 1, "A", "T"),
                new Answer(false, true, 1, "P3", 1, 2, "A", "T"),
                new Answer(false, true, 1, "P3", 1, 3, "A", "T"),
                new Answer(false, true, 1, "P3", 2, 1, "A", "T"),
                new Answer(false, true, 1, "P3", 2, 2, "A", "T"),
                new Answer(false, true, 1, "P3", 2, 3, "A", "T"),
                new Answer(false, true, 1, "P3", 3, 1, "A", "T"),
                new Answer(false, true, 1, "P3", 3, 2, "A", "T"),
                new Answer(false, true, 1, "P3", 3, 3, "A", "T"),

                // P4
                new Answer(true, true, 1, "P4", 1, 1, "A", "T"),
                new Answer(true, true, 1, "P4", 1, 2, "A", "T"),
                new Answer(true, true, 1, "P4", 1, 3, "A", "T"),

                // P5
                new Answer(true, true, 1, "P5", 2, 1, "A", "T"),
                new Answer(true, true, 1, "P5", 2, 2, "A", "T"),
                new Answer(true, true, 1, "P5", 2, 3, "A", "T"),

                // P6
                new Answer(true, true, 1, "P6", 3, 1, "A", "T"),
                new Answer(true, true, 1, "P6", 3, 2, "A", "T"),
                new Answer(true, true, 1, "P6", 3, 3, "A", "T")
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
