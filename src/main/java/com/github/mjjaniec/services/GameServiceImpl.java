package com.github.mjjaniec.services;

import com.github.mjjaniec.model.GameLevel;
import com.github.mjjaniec.model.Player;
import com.github.mjjaniec.model.Quiz;
import com.github.mjjaniec.model.RoundMode;
import com.github.mjjaniec.util.R;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GameServiceImpl implements GameService {
    private final BigScreenNavigator bigScreenNavigator;
    private final PlayerNavigator playerNavigator;
    private final PlayerStore playerStore;
    private Quiz quiz;

    public GameServiceImpl(BigScreenNavigator bigScreenNavigator, PlayerNavigator playerNavigator, PlayerStore playerStore) {
        this.bigScreenNavigator = bigScreenNavigator;
        this.playerNavigator = playerNavigator;
        this.playerStore = playerStore;
    }

    @Override
    public GameLevel currentLevel() {
        return new GameLevel(1, 1, 4, 2, RoundMode.EVERYBODY);
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
    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
        playerNavigator.navigatePlayers(R.Player.Join.IT);
    }

    @Override
    public Quiz quiz() {
        return quiz;
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
