package com.github.mjjaniec.views.bigscreen;


import com.github.mjjaniec.model.GameStage;
import com.github.mjjaniec.model.MainSet;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface BigScreenRoute {

    // todo remove
    default Optional<GameStage.RoundPiece> testPiece() {
        return Optional.of(new GameStage.RoundPiece(
                new GameStage.PieceNumber(3, 10),
                new MainSet.Piece("Red Hot Chilli Pepper", "Callifornication", MainSet.Instrument.Bass, null, null, Set.of()),
                List.of(GameStage.PieceStage.LISTEN, GameStage.PieceStage.ANSWER)
        ));
//        return Optional.empty();
    }

    // todo remove
    default Optional<GameStage.RoundInit> testInit() {
        return Optional.of(new GameStage.RoundInit(new GameStage.RoundNumber(1,3),
                MainSet.Difficulty.Easy,
                List.of(),
                new GameStage.RoundSummary(new GameStage.RoundNumber(1,3))
        ));
//        return Optional.empty();
    }

}
