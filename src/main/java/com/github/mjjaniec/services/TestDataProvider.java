package com.github.mjjaniec.services;

import com.github.mjjaniec.config.ApplicationConfig;
import com.github.mjjaniec.model.GameStage;
import com.github.mjjaniec.model.MainSet;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class TestDataProvider {

    private final ApplicationConfig config;

    public TestDataProvider(ApplicationConfig config) {
        this.config = config;
    }

    public Optional<GameStage.RoundPiece> piece() {
        return Optional.of(new GameStage.RoundPiece(
                4,
                new GameStage.PieceNumber(3, 10),
                new MainSet.Piece( "Red Hot Chilli Pepper", "Callifornication", MainSet.Instrument.Bass, null, null, Set.of()),
                List.of(GameStage.PieceStage.LISTEN, GameStage.PieceStage.ANSWER)
        )).filter(event -> config.testData());

    }

    public Optional<GameStage.RoundInit> init() {
        return Optional.of(new GameStage.RoundInit(new GameStage.RoundNumber(1,3),
                MainSet.Difficulty.Easy,
                List.of(),
                new GameStage.RoundSummary(new GameStage.RoundNumber(1,3))
        )).filter(event -> config.testData());
    }
}
