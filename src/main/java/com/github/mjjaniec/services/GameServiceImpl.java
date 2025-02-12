package com.github.mjjaniec.services;

import com.github.mjjaniec.model.GameStage;
import com.github.mjjaniec.model.MainSet;
import com.github.mjjaniec.model.Player;
import com.github.mjjaniec.views.player.JoinView;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GameServiceImpl implements GameService, MaestroInterface {
    private final BigScreenNavigator bigScreenNavigator;
    private final PlayerNavigator playerNavigator;
    private final PlayerStore playerStore;
    private MainSet quiz;
    private GameStage<?, ?> stage;


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
        this.stage = new GameStage.Invite();
        playerNavigator.navigatePlayers(JoinView.class);
    }

    @Override
    public boolean addPlayer(String name) {
        boolean result = playerStore.addPlayer(name);
        if (result) {
            bigScreenNavigator.refreshPlayers();

        }
        return result;
    }

    @Override
    public void removePlayer(Player player) {
        playerStore.removePlayer(player);
        bigScreenNavigator.refreshPlayers();
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
        bigScreenNavigator.navigateBigScreen(stage.getBigScreenView());
    }
}
