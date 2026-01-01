package com.github.mjjaniec.lmq.model;

import com.github.mjjaniec.lmq.model.MainSet.RoundMode;
import com.google.common.collect.Streams;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class StageSet {

    private final List<GameStage> mainStages;

    public StageSet(MainSet quiz) {
        mainStages = Stream.<Stream<GameStage>>of(
                Stream.of(new GameStage.Invite()),
                Streams.mapWithIndex(quiz.levels().stream(), (level, index) -> roundStages(level, index, quiz)),
                Stream.of(new GameStage.PlayOff()),
                Stream.of(new GameStage.WrapUp())
        ).flatMap(Function.identity()).toList();
    }

    public GameStage initStage() {
        return mainStages.getFirst();
    }

    public GameStage.RoundSummary lastRoundSummary() {
        return ((GameStage.RoundInit) mainStages.get(mainStages.size() - 3)).roundSummary();
    }

    public GameStage.PlayOff playOff() {
        return (GameStage.PlayOff) mainStages.get(mainStages.size() - 2);
    }

    public GameStage.WrapUp wrapUpStage() {
        return (GameStage.WrapUp) mainStages.getLast();
    }

    public Optional<GameStage.RoundInit> roundInit(int roundNumber) {
        if (roundNumber == 0 || roundNumber >= mainStages.size() - 1) {
            return Optional.empty();
        }
        return switch (mainStages.get(roundNumber)) {
            case GameStage.RoundInit roundInit -> Optional.of(roundInit);
            default -> Optional.empty();
        };
    }

    public List<GameStage> topLevelStages() {
        return mainStages;
    }

    private GameStage.RoundInit roundStages(MainSet.LevelPieces level, long roundIndex, MainSet quiz) {
        return new GameStage.RoundInit(
                new GameStage.RoundNumber((int) roundIndex + 1, quiz.levels().size()),
                level.level(),
                Streams.mapWithIndex(level.pieces().stream(), (piece, index) ->
                        new GameStage.RoundPiece(
                                (int) roundIndex + 1,
                                new GameStage.PieceNumber((int) index + 1, level.pieces().size()),
                                piece,
                                switch (level.level()) {
                                    case RoundMode.FIRST -> List.of(GameStage.PieceStage.PLAY, GameStage.PieceStage.REVEAL);
                                    case RoundMode.ONION -> List.of(GameStage.PieceStage.ONION_LISTEN, GameStage.PieceStage.REVEAL);
                                    case RoundMode.EVERYBODY ->  List.of(GameStage.PieceStage.LISTEN, GameStage.PieceStage.REVEAL);
                                })
                ).toList(),
                new GameStage.RoundSummary(new GameStage.RoundNumber((int) roundIndex + 1, quiz.levels().size()))
        );
    }
}
