package com.github.mjjaniec.services;

import com.github.mjjaniec.model.*;
import com.github.mjjaniec.util.R;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GameServiceImpl implements GameService {
    private final BigScreenNavigator bigScreenNavigator;
    private final PlayerNavigator playerNavigator;
    private final PlayerStore playerStore;
    private MainSet quiz;


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
    public void startListening() {

    }

    @Override
    public void endListening() {

    }

    @Override
    public void advance() {

    }

    @Override
    public MainSet quiz() {
        return quiz;
    }

    @Override
    public void setSet(MainSet set) {
        this.quiz = set;
        playerNavigator.navigatePlayers(R.Player.Join.IT);
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
    public List<Player> getPlayers() {
        return playerStore.getPlayers();
    }
}
