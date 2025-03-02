package com.github.mjjaniec.services;

import com.github.mjjaniec.model.GameStage;
import com.github.mjjaniec.model.MainSet;
import com.github.mjjaniec.model.Player;
import com.github.mjjaniec.model.StageSet;
import com.github.mjjaniec.stores.CustomMessageStore;
import com.github.mjjaniec.stores.PlayerStore;
import com.github.mjjaniec.stores.QuizStore;
import com.github.mjjaniec.stores.StageStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class GameServiceImpl implements GameService, MaestroInterface {
    private final BigScreenNavigator bigScreenNavigator;
    private final PlayerNavigator playerNavigator;
    private final PlayerStore playerStore;
    private final QuizStore quizStore;
    private final CustomMessageStore messageStore;
    private final StageStore stageStore;
    private MainSet quiz;
    private GameStage stage;
    private StageSet stageSet;

    public GameServiceImpl(BigScreenNavigator bigScreenNavigator, PlayerNavigator playerNavigator,
                           PlayerStore playerStore, QuizStore quizStore, CustomMessageStore messageStore, StageStore stageStore) {
        this.bigScreenNavigator = bigScreenNavigator;
        this.playerNavigator = playerNavigator;
        this.playerStore = playerStore;
        this.quizStore = quizStore;
        this.messageStore = messageStore;
        this.stageStore = stageStore;

        quizStore.getQuiz().ifPresent(this::initGame);
        stageStore.readStage(stageSet).ifPresentOrElse(this::setStage, () -> {
            if (quiz != null) setStage(stageSet.initStage());
        });
    }

    @Override
    public boolean hasPlayer(Player player) {
        return playerStore.hasPlayer(player);
    }


    @Override
    public boolean isGameStarted() {
        return quiz != null;
    }

    @Override
    public void initGame(MainSet set) {
        quizStore.setQuiz(set);
        this.quiz = set;
        this.stageSet = new StageSet(set);
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
    public GameStage stage() {
        return stage;
    }

    @Override
    public List<Player> getPlayers() {
        return playerStore.getPlayers();
    }

    @Override
    public void reset() {
        quizStore.clearQuiz();
        quiz = null;
        stage = null;
        stageStore.clearStage();
    }

    @Override
    public void setStage(GameStage gameStage) {
        this.stage = gameStage;
        stageStore.saveStage(gameStage);
        playerNavigator.navigatePlayers(gameStage.playerView());
        bigScreenNavigator.navigateBigScreen(gameStage.bigScreenView());
    }

    @Override
    public StageSet stageSet() {
        return stageSet;
    }

    @Override
    public void reportResult(Optional<Player> player, boolean artist, boolean title, boolean bonus) {
        // ignore for now
    }

    @Override
    public void setCustomMessage(String customMessage) {
        messageStore.setMessage(customMessage);
        bigScreenNavigator.refreshBigScreen();
    }

    @Override
    public void clearCustomMessage() {
        messageStore.clearMessage();
        bigScreenNavigator.refreshBigScreen();
    }

    @Override
    public Optional<String> customMessage() {
        return messageStore.readMessage();
    }
}
