package com.github.mjjaniec.services;

import com.github.mjjaniec.model.*;
import com.github.mjjaniec.stores.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class GameServiceImpl implements GameService, MaestroInterface {
    private final Navigator navigator;
    private final PlayerStore playerStore;
    private final QuizStore quizStore;
    private final CustomMessageStore messageStore;
    private final StageStore stageStore;
    private final AnswerStore answerStore;
    private final FeedbackStore feedbackStore;
    private MainSet quiz;
    private GameStage stage;
    private StageSet stageSet;

    private final List<Player> slackers = new ArrayList<>();

    public GameServiceImpl(Navigator navigator,
                           PlayerStore playerStore,
                           QuizStore quizStore,
                           CustomMessageStore messageStore,
                           StageStore stageStore,
                           AnswerStore answerStore,
                           FeedbackStore feedbackStore) {
        this.navigator = navigator;
        this.playerStore = playerStore;
        this.quizStore = quizStore;
        this.messageStore = messageStore;
        this.stageStore = stageStore;
        this.answerStore = answerStore;
        this.feedbackStore = feedbackStore;

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
        this.stage = stageSet.initStage();
    }

    @Override
    public boolean addPlayer(String name) {
        boolean result = playerStore.addPlayer(name);
        if (result) {
            navigator.refreshPlayerLists();
        }
        return result;
    }

    @Override
    public void removePlayer(Player player) {
        playerStore.removePlayer(player);
        navigator.refreshAllPlayers();
        navigator.refreshPlayerLists();
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
        quiz = null;
        quizStore.clearQuiz();
        stage = null;
        stageStore.clearStage();
        answerStore.clearAnswers();
    }

    private void initAnswers() {
        slackers.clear();
        slackers.addAll(playerStore.getPlayers());
    }

    @Override
    public void setStage(GameStage gameStage) {
        this.stage = gameStage;
        stageStore.saveStage(gameStage);
        navigator.navigatePlayers(gameStage.playerView());
        navigator.navigateBigScreen(gameStage.bigScreenView());
        navigator.refreshProgressBar();

        gameStage.asPiece().map(GameStage.RoundPiece::getCurrentStage).ifPresent(pieceStage -> {
            switch (pieceStage) {
                case ANSWER -> initAnswers();
                case PLAY -> navigator.refreshPlay();
                default -> {}
            }
        });

    }


    @Override
    public StageSet stageSet() {
        return stageSet;
    }

    @Override
    public void reportResult(Player player, boolean artist, boolean title, boolean bonus) {
        stage.asPiece().ifPresentOrElse(piece -> {
            answerStore.saveAnswer(new Answer(artist, title, bonus ? 2 : 1, player.name(), piece.roundNumber, piece.pieceNumber.number()));
            slackers.remove(player);
            navigator.refreshSlackersList();
        }, () -> log.error("Report result called in wrong state (expected Piece but it is: {}", stage));
    }

    @Override
    public void setCustomMessage(String customMessage) {
        messageStore.setMessage(customMessage);
        navigator.refreshBigScreen();
    }

    @Override
    public void clearCustomMessage() {
        messageStore.clearMessage();
        navigator.refreshBigScreen();
    }

    @Override
    public Optional<String> customMessage() {
        return messageStore.readMessage();
    }

    @Override
    public List<Player> getSlackers() {
        return List.copyOf(slackers);
    }

    @Override
    public int getCurrentPlayerPoints(Player player) {
        return stage.asPiece().map(piece -> piecePoints(player, piece))
                .or(() -> stage.asRoundSummary().map(summary -> roundPoints(player, summary)))
                .orElse(0);
    }

    @Override
    public Map<String, Map<Integer, Integer>> totalPoints() {
        Map<String, Map<Integer, Integer>> result = new HashMap<>();
        answerStore.allAnswers().forEach(answer ->
                stageSet.roundInit(answer.piece()).map(GameStage.RoundInit::difficulty).map(d -> d.points).ifPresent(points -> {
                            var players = result.computeIfAbsent(answer.player(), k -> new HashMap<>());
                            players.put(answer.round(), players.getOrDefault(answer.round(), 0) + forAnswer(points, answer));
                        }
                )
        );
        return result;
    }

    @Override
    public void saveFeedback(String value) {
        feedbackStore.saveFeedback(value);
    }

    @Override
    public List<String> getFeedbacks() {
        return feedbackStore.readFeedback();
    }

    private int roundPoints(Player player, GameStage.RoundSummary summary) {
        return stageSet.roundInit(summary.roundNumber().number()).map(GameStage.RoundInit::difficulty).map(d -> d.points)
                .map(points ->
                        answerStore.playerAnswers(player.name(), summary.roundNumber().number())
                                .mapToInt(answer -> forAnswer(points, answer))
                                .sum()
                ).orElse(0);
    }

    private int piecePoints(Player player, GameStage.RoundPiece piece) {
        return answerStore.playerAnswer(player.name(), piece.roundNumber, piece.pieceNumber.number()).map(
                answer -> stageSet.roundInit(piece.roundNumber).map(
                        round -> forAnswer(round.difficulty().points, answer)
                ).orElse(0)
        ).orElse(0);
    }

    private int forAnswer(MainSet.RoundPoints roundPoints, Answer answer) {
        return answer.bonus() * b2i(answer.title()) * roundPoints.title()
                + answer.bonus() * b2i(answer.artist()) * roundPoints.artist();
    }

    private int b2i(boolean b) {
        return b ? 1 : 0;
    }

}
