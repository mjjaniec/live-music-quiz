package com.github.mjjaniec.services;

import com.github.mjjaniec.model.*;
import com.github.mjjaniec.stores.*;
import com.github.mjjaniec.views.bigscreen.RevealView;
import com.github.mjjaniec.views.player.PieceResultView;
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
    private final PlayOffStore playOffStore;
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
                           FeedbackStore feedbackStore, PlayOffStore playOffStore) {
        this.navigator = navigator;
        this.playerStore = playerStore;
        this.quizStore = quizStore;
        this.messageStore = messageStore;
        this.stageStore = stageStore;
        this.answerStore = answerStore;
        this.feedbackStore = feedbackStore;
        this.playOffStore = playOffStore;

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
        playOffStore.clearPlayOffs();
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


        gameStage.asPiece().ifPresent(piece -> {
            switch (piece.getCurrentStage()) {
                case ANSWER -> initAnswers();
                case PLAY -> {
                    if (piece.isCompleted()) {
                        navigator.navigatePlayers(PieceResultView.class);
                        navigator.navigateBigScreen(RevealView.class);
                    } else {
                        navigator.refreshPlay();
                    }
                }
                default -> {
                }
            }
        });
        gameStage.asPlayOff().ifPresent(playOff -> {
            if (playOff.getPlayOff() == null) {
                playOffStore.clearPlayOffs();
                slackers.clear();
                slackers.addAll(playerStore.getPlayers());
            }
            navigator.refreshPlayOff();
        });
        gameStage.asWrapUp().ifPresent(ignored -> navigator.refreshWrapUp());
    }

    @Override
    public GameStage.Display minimalDisplay() {
        int playersCount = playerStore.getPlayers().size();
        if (playersCount >= 7) {
            return GameStage.Display.SIXTH;
        } else if (playersCount >= 4) {
            return GameStage.Display.FOURTH;
        } else if (playersCount >= 3) {
            return GameStage.Display.THIRD_PODIUM;
        } else {
            return GameStage.Display.FULL_TABLE;
        }
    }

    @Override
    public StageSet stageSet() {
        return stageSet;
    }

    @Override
    public void reportResult(Player player, boolean artist, boolean title, int bonus) {
        stage.asPiece().ifPresentOrElse(piece -> {
            answerStore.saveAnswer(new Answer(artist, title, bonus, player.name(), piece.roundNumber, piece.pieceNumber.number()));
            slackers.remove(player);
            navigator.refreshSlackersList();
        }, () -> log.error("Report result called in wrong state (expected Piece but it is: {}", stage));
    }

    @Override
    public void savePlayOff(Player player, int value) {
        playOffStore.savePlayOff(player, value);
        slackers.remove(player);
        navigator.refreshSlackersList();
    }

    @Override
    public void setCustomMessage(String customMessage) {
        messageStore.setMessage(customMessage);
        navigator.refreshCustomMessage();
    }

    @Override
    public void clearCustomMessage() {
        messageStore.clearMessage();
        navigator.refreshCustomMessage();
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

    @Override
    public synchronized void raise(Player player) {
        stage.asPiece().ifPresent(piece -> {
            if (piece.getCurrentResponder() == null) {
                piece.setCurrentResponder(player.name());
                stageStore.saveStage(stage);
                navigator.refreshPlay();
            }
        });
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
