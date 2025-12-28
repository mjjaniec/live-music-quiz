package com.github.mjjaniec.lmq.services;

import com.github.mjjaniec.lmq.model.*;
import com.github.mjjaniec.lmq.stores.*;
import com.google.common.collect.Streams;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

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
                           PlayOffTaskStore playOffTaskStore) {
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
        return quiz != null;
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
                    if (previousStage!= gameStage) {
                        clearCurrentPoints(piece);
                    }
                }
                case PLAY -> {
                    if (previousStage!= gameStage) {
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
    public void reportResult(Player player, boolean artist, boolean title, int bonus, @Nullable String actualArtist, @Nullable String actualTitle) {
        Optional.ofNullable(stage).flatMap(GameStage::asPiece).ifPresentOrElse(piece -> {
            answerStore.saveAnswer(new Answer(artist, title, bonus, player.name(), piece.roundNumber, piece.pieceNumber.number(), actualArtist, actualTitle));
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
        if (stageSet == null) {
            return 0;
        }
        return Optional.ofNullable(stage).flatMap(GameStage::asPiece).map(piece -> piecePoints(player, piece, stageSet))
                .or(() -> Optional.ofNullable(stage).flatMap(GameStage::asRoundSummary).map(summary -> roundPoints(player, summary, stageSet)))
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
        Objects.requireNonNull(stageSet);
        Objects.requireNonNull(stage);
        Map<String, Map<Integer, Integer>> byRounds = totalPoints(stageSet);
        Map<String, Integer> altogether = byRounds.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().values().stream().mapToInt(x -> x).sum()
        ));
        int playOffTarget = playOffTaskStore.getPlayOffTask(playOffs).map(PlayOffs.PlayOff::value).orElse(0);
        Map<String, Integer> playOffsDiffs = new HashMap<>();
        Map<String, Integer> playOffsValues = new HashMap<>();
        playOffTaskStore.getPlayOffTask(playOffs).ifPresent(_ -> {
            playOffsValues.putAll(playOffStore.getPlayOffs());
            playOffsValues.forEach((key, value) -> playOffsDiffs.put(key, Math.abs(value - playOffTarget)));
        });

        List<String> order = getPlayers().stream().map(Player::name).sorted((a, b) -> {
            int aPoints = altogether.getOrDefault(a, 0);
            int bPoints = altogether.getOrDefault(b, 0);
            int aDiff = playOffsDiffs.getOrDefault(a, 0);
            int bDiff = playOffsDiffs.getOrDefault(b, 0);
            if (aPoints != bPoints) {
                return bPoints - aPoints;
            }
            if (aDiff != bDiff) {
                return aDiff - bDiff;
            } else {
                return a.compareTo(b);
            }
        }).toList();

        int rounds = (int) stageSet.topLevelStages().stream().filter(stage -> stage.asRoundInit().isPresent()).count();
        int currentRound = stage.asRoundSummary().map(s -> s.roundNumber().number()).orElse(rounds);

        if (order.isEmpty()) {
            return new Results(rounds, currentRound, playOffTarget, List.of());
        }

        int position = 1;
        int count = 1;
        Map<String, Integer> positions = new HashMap<>();
        String previous = order.getFirst();
        positions.put(previous, position);
        int bestDiff = Integer.MAX_VALUE;
        for (String p : order) {
            if (p.equals(previous)) continue;
            if (Objects.equals(altogether.getOrDefault(p, 0), altogether.getOrDefault(previous, 0))
                && Objects.equals(playOffsDiffs.getOrDefault(p, 0), playOffsDiffs.getOrDefault(previous, 0))) {
                positions.put(p, positions.get(previous));
                count += 1;
            } else {
                position += count;
                positions.put(p, position);
                count = 1;
            }
            if (position > 3) {
                bestDiff = Math.min(bestDiff, playOffsDiffs.getOrDefault(p, Integer.MAX_VALUE));
            }
            previous = p;
        }

        int finalBestDiff = bestDiff;

        return new Results(rounds, currentRound, playOffTarget, Streams.mapWithIndex(order.stream(), (name, index) -> {
            int pos = positions.get(name);
            int ordinal = (int) index + 1;
            Optional<Results.Award> award = switch (pos) {
                case 1 -> Optional.of(Results.Award.FIRST);
                case 2 -> Optional.of(Results.Award.SECOND);
                case 3 -> Optional.of(Results.Award.THIRD);
                default ->
                        Optional.of(Results.Award.PLAY_OFF).filter(ignored -> playOffsDiffs.getOrDefault(name, -1) == finalBestDiff);
            };
            return new Results.Row(name, ordinal, pos, award, byRounds.getOrDefault(name, Map.of()), playOffsValues.getOrDefault(name, -1), altogether.getOrDefault(name, 0));
        }).toList());
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

    private Map<String, Map<Integer, Integer>> totalPoints(StageSet set) {
        Map<String, Map<Integer, Integer>> result = new HashMap<>();
        answerStore.allAnswers().forEach(answer ->
                set.roundInit(answer.round()).map(GameStage.RoundInit::roundMode).ifPresent(mode -> {
                            var players = result.computeIfAbsent(answer.player(), _ -> new HashMap<>());
                            players.put(answer.round(), players.getOrDefault(answer.round(), 0) + forAnswer(mode, answer));
                        }
                )
        );
        return result;
    }

    private int roundPoints(Player player, GameStage.RoundSummary summary, StageSet set) {
        return set.roundInit(summary.roundNumber().number()).map(GameStage.RoundInit::roundMode)
                .map(mode ->
                        answerStore.playerAnswers(player.name(), summary.roundNumber().number())
                                .mapToInt(answer -> forAnswer(mode, answer))
                                .sum()
                ).orElse(0);
    }

    private int piecePoints(Player player, GameStage.RoundPiece piece, StageSet set) {
        return answerStore.playerAnswer(player.name(), piece.roundNumber, piece.pieceNumber.number()).map(
                answer -> set.roundInit(piece.roundNumber).map(
                        round -> forAnswer(round.roundMode(), answer)
                ).orElse(0)
        ).orElse(0);
    }

    private int forAnswer(MainSet.RoundMode roundMode, Answer answer) {
        return answer.bonus() * b2i(answer.title()) * roundMode.titlePoints
               + answer.bonus() * b2i(answer.artist()) * roundMode.artistPoints;
    }

    private void clearCurrentPoints(GameStage.RoundPiece piece) {
        getPlayers().forEach(player -> answerStore.deleteAnswer(player.name(), piece.roundNumber, piece.pieceNumber.number()));
    }

    private int b2i(boolean b) {
        return b ? 1 : 0;
    }

}
