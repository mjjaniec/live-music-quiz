package com.github.mjjaniec.services;

import com.github.mjjaniec.model.GameStage;
import com.github.mjjaniec.model.GameStage.RoundInit;
import com.github.mjjaniec.model.GameStage.RoundNumber;
import com.github.mjjaniec.model.MainSet;
import com.github.mjjaniec.model.Player;
import com.google.common.collect.Streams;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

@Component
public class GameServiceImpl implements GameService, MaestroInterface {
    private final BigScreenNavigator bigScreenNavigator;
    private final PlayerNavigator playerNavigator;
    private final PlayerStore playerStore;
    private MainSet quiz;
    private GameStage<?, ?> stage;
    private List<GameStage<?, ?>> _allStages;


    public GameServiceImpl(BigScreenNavigator bigScreenNavigator, PlayerNavigator playerNavigator, PlayerStore playerStore) {
        this.bigScreenNavigator = bigScreenNavigator;
        this.playerNavigator = playerNavigator;
        this.playerStore = playerStore;
    }

    @Override
    public boolean hasPlayer(Player player) {
        return playerStore.hasPlayer(player);
    }


    @Override
    public MainSet quiz() {
        return quiz;
    }

    @Override
    public void initGame(MainSet set) {
        this.quiz = set;
        this._allStages = computeAllStages();
        setStage(_allStages.getFirst());
    }

    @Override
    public boolean addPlayer(String name) {
        boolean result = playerStore.addPlayer(name);
        if (result) {
            bigScreenNavigator.refreshPlayerLists();

        }
        return result;
    }

    @Override
    public void removePlayer(Player player) {
        playerStore.removePlayer(player);
        playerNavigator.refreshAllPlayers();
        bigScreenNavigator.refreshPlayerLists();
    }

    @Override
    public GameStage<?, ?> stage() {
        return stage;
    }

    @Override
    public List<Player> getPlayers() {
        return playerStore.getPlayers();
    }

    @Override
    public void reset() {
        quiz = null;
        stage = null;
    }

    @Override
    public void setStage(GameStage<?, ?> gameStage) {
        this.stage = gameStage;
        playerNavigator.navigatePlayers(stage.playerView());
        bigScreenNavigator.navigateBigScreen(stage.bigScreenView());
    }

    @Override
    public List<GameStage<?, ?>> allStages() {
        return _allStages;
    }

    private List<GameStage<?, ?>> computeAllStages() {
        if (quiz == null) return List.of();
        return Stream.<Stream<GameStage<?, ?>>>of(
                Stream.of(new GameStage.Invite()),
                Streams.mapWithIndex(quiz.levels().stream(), this::roundStages),
                Stream.of(new GameStage.WrapUp())
        ).flatMap(Function.identity()).toList();
    }

    private RoundInit roundStages(MainSet.LevelPieces level, long roundIndex) {
        return new RoundInit(
                new RoundNumber(roundIndex + 1, quiz.levels().size()),
                level.level(),
                Streams.mapWithIndex(level.pieces().stream(), (piece, index) ->
                        new GameStage.RoundPiece(
                                new GameStage.PieceNumber(index + 1, level.pieces().size()),
                                piece,
                                List.of(GameStage.PieceStage.LISTEN, GameStage.PieceStage.REPORT)
                        )
                ).toList(),
                new GameStage.RoundSummary(new RoundNumber(roundIndex + 1, quiz.levels().size()))
        );
    }
}
