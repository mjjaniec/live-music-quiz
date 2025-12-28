package com.github.mjjaniec.lmq.services;

import com.github.mjjaniec.lmq.model.*;
import com.github.mjjaniec.lmq.stores.*;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Component
public class GameServiceImpl implements GameService, MaestroInterface {
    private final Navigator navigator;
    private final SpreadsheetLoader spreadsheetLoader;
    private final PlayerStore playerStore;
    private final QuizStore quizStore;
    private final CustomMessageStore messageStore;
    private final StageStore stageStore;
    private final AnswerStore answerStore;
    private final FeedbackStore feedbackStore;
    private final PlayOffStore playOffStore;
    private final PlayOffTaskStore playOffTaskStore;
    private final PointsCounter pointsCounter;
    private @Nullable MainSet quiz;
    private @Nullable GameStage stage;
    private @Nullable StageSet stageSet;
    private @Nullable PlayOffs playOffs;

    private final List<Player> slackers = new ArrayList<>();

    public GameServiceImpl(Navigator navigator,
                           SpreadsheetLoader spreadsheetLoader,
                           PlayerStore playerStore,
                           QuizStore quizStore,
                           CustomMessageStore messageStore,
                           StageStore stageStore,
                           AnswerStore answerStore,
                           FeedbackStore feedbackStore,
                           PlayOffStore playOffStore,
                           PlayOffTaskStore playOffTaskStore,
                           PointsCounter pointsCounter) {
        this.navigator = navigator;
        this.spreadsheetLoader = spreadsheetLoader;
        this.playerStore = playerStore;
        this.quizStore = quizStore;
        this.messageStore = messageStore;
        this.stageStore = stageStore;
        this.answerStore = answerStore;
        this.feedbackStore = feedbackStore;
        this.playOffStore = playOffStore;
        this.playOffTaskStore = playOffTaskStore;
        this.pointsCounter = pointsCounter;

        quizStore.getQuiz().ifPresent(this::initGame);
        stageStore.readStage(stageSet).ifPresentOrElse(this::setStage, () -> {
            if (quiz != null && stageSet != null) setStage(stageSet.initStage());
        });
    }

    @Override
    public boolean hasPlayer(Player player) {
        return playerStore.hasPlayer(player);
    }


    @Override
    public boolean isGameStarted() {
        return quiz != null && stageSet != null;
    }

    @Override
    public void initGame(MainSet set) {
        quizStore.setQuiz(set);
        this.quiz = set;
        this.stageSet = new StageSet(set);
        this.stage = stageSet.initStage();
        playOffs = spreadsheetLoader.loadPlayOffs();
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
    public @Nullable GameStage stage() {
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
        playOffTaskStore.clearPlayOffTask();
        playerStore.clearPlayers();
        messageStore.clearMessage();
    }

    private void initSlackers(GameStage.RoundPiece piece) {
        slackers.clear();
        playerStore.getPlayers().stream()
                .filter(player -> answerStore.playerAnswer(player.name(), piece.roundNumber, piece.pieceNumber.number()).isEmpty())
                .forEach(slackers::add);
    }

    @Override
    public void setStage(GameStage gameStage) {
        GameStage previousStage = this.stage;
        this.stage = gameStage;
        stageStore.saveStage(gameStage);

        gameStage.asPiece().ifPresent(piece -> {
            switch (piece.getCurrentStage()) {
                case LISTEN -> {
                    initSlackers(piece);
                    navigator.refreshBonus();
                    if (previousStage != gameStage) {
                        clearCurrentPoints(piece);
                    }
                }
                case PLAY -> {
                    if (previousStage != gameStage) {
                        clearCurrentPoints(piece);
                        piece.clear();
                        stageStore.saveStage(stage);
                    } else {
                        navigator.refreshPlay();
                    }
                }
                default -> {
                }
            }
        });
        gameStage.asPlayOff().ifPresent(playOff -> {
            if (!playOff.isPerformed()) {
                slackers.clear();
                slackers.addAll(playerStore.getPlayers());
            }
            navigator.refreshPlayOff();
        });
        gameStage.asWrapUp().ifPresent(ignored -> navigator.refreshWrapUp());

        navigator.navigatePlayers(gameStage.playerView());
        navigator.navigateBigScreen(gameStage.bigScreenView());
        navigator.refreshProgressBar();
    }

    @Override
    public GameStage.Display minimalDisplay() {
        int playersCount = playerStore.getPlayers().size();
        if (playersCount >= 7) {
            return GameStage.Display.SIXTH;
        } else if (playersCount >= 4) {
            return GameStage.Display.FOURTH;
        } else if (playersCount == 3) {
            return GameStage.Display.EMPTY_PODIUM;
        } else {
            return GameStage.Display.FULL_TABLE;
        }
    }

    @Override
    public @Nullable StageSet stageSet() {
        return stageSet;
    }

    @Override
    public Optional<Answer> getCurrentAnswer(Player player) {
        return Optional.ofNullable(stage).flatMap(GameStage::asPiece)
                .flatMap(piece -> answerStore.playerAnswer(player.name(), piece.roundNumber, piece.pieceNumber.number()));
    }


    @Override
    public synchronized void reportResult(Player player, boolean artist, boolean title, @Nullable String actualArtist, @Nullable String actualTitle) {
        if (stage == null || stageSet == null) {
            log.error("reportResult called when stage or stageSet is not set");
        } else {
            stage.asPiece().ifPresentOrElse(piece -> {

                if (title) {
                    piece.incrementTitleAnswered();
                }
                if (artist) {
                    piece.incrementArtistAnswered();
                }
                if (!artist || !title) {
                    piece.addFailedResponder(player.name());
                } else {
                    piece.setCurrentResponder(null);
                }

                MainSet.RoundMode mode = stageSet.roundInit(piece.roundNumber).map(GameStage.RoundInit::roundMode).orElseThrow();
                switch (mode) {
                    case FIRST -> {
                        piece.setBonus(1 + piece.getFailedResponders().size());
                        if (piece.isCompleted()) piece.setCurrentStage(GameStage.PieceStage.REVEAL);
                    }
                    case ONION -> piece.setBonus(onionBonus(piece.getArtistAnswered() + piece.getTitleAnswered()));
                }

                answerStore.saveAnswer(new Answer(artist, title, piece.getBonus(), player.name(), piece.roundNumber, piece.pieceNumber.number(), actualArtist, actualTitle));
                setStage(piece);

                slackers.remove(player);
                navigator.refreshSlackersList();
            }, () -> log.error("Report result called in wrong state (expected Piece but it is: {}", stage));
        }
    }

    private int onionBonus(int correctAnswers) {
        if (correctAnswers <= 2) {
            return 4;
        } else if (correctAnswers <= 6) {
            return 3;
        } else if (correctAnswers <= 12) {
            return 2;
        } else {
            return 1;
        }
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
    public synchronized int getCurrentPlayerPoints(Player player) {
        if (stage == null || stageSet == null) {
            return 0;
        }

        return stage.asPiece()
                .map(piece -> {
                    Optional<Answer> pieceAnswer = answerStore.playerAnswer(player.name(), piece.roundNumber, piece.pieceNumber.number());
                    return pointsCounter.piecePoints(piece, stageSet, pieceAnswer);
                })
                .or(() -> stage.asRoundSummary()
                        .map(summary -> {
                            Stream<Answer> playerAnswers = answerStore.playerAnswers(player.name(), summary.roundNumber().number());
                            return pointsCounter.roundPoints(summary, stageSet, playerAnswers);
                        }))
                .orElse(0);
    }

    @Override
    public void setPlayOffTask(PlayOffs.PlayOff playOff) {
        playOffTaskStore.savePlayOffTask(playOff);
        playOffStore.clearPlayOffs();
    }

    @Override
    public void clearPlayOffTask() {
        playOffTaskStore.clearPlayOffTask();
    }

    @Override
    public Optional<PlayOffs.PlayOff> playOffTask() {
        return playOffTaskStore.getPlayOffTask(playOffs);
    }

    @Override
    public Results results() {
        if (stage == null || stageSet == null || playOffs == null) {
            return new Results(0, 0, 0, List.of());
        }
        return pointsCounter.results(stage, stageSet,
                playerStore.getPlayers(),
                answerStore.allAnswers(),
                playOffTaskStore.getPlayOffTask(playOffs),
                playOffStore.getPlayOffs());
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
        Optional.ofNullable(stage).flatMap(GameStage::asPiece).ifPresent(piece -> {
            if (piece.getCurrentResponder() == null) {
                piece.setCurrentResponder(player.name());
                stageStore.saveStage(stage);
                navigator.refreshPlay();
            }
        });
    }

    private void clearCurrentPoints(GameStage.RoundPiece piece) {
        getPlayers().forEach(player -> answerStore.deleteAnswer(player.name(), piece.roundNumber, piece.pieceNumber.number()));
    }
}
