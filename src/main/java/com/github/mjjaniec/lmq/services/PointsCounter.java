package com.github.mjjaniec.lmq.services;

import com.github.mjjaniec.lmq.model.*;
import com.google.common.collect.Streams;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class PointsCounter {


    public Results results(GameStage.RoundSummary stage, StageSet stageSet,
                           List<Player> players, Stream<Answer> allAnswers,
                           Optional<PlayOffs.PlayOff> playOffTask, Map<String, Integer> playOffsValues) {
        Map<String, Map<Integer, Integer>> byRounds = totalPoints(stageSet, allAnswers);
        Map<String, Integer> altogether = byRounds.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().values().stream().mapToInt(x -> x).sum()
        ));
        int playOffTarget = playOffTask.map(PlayOffs.PlayOff::value).orElse(0);
        Map<String, Integer> playOffsDiffs = new HashMap<>();
        playOffTask.ifPresent(_ -> playOffsValues.forEach((key, value) -> playOffsDiffs.put(key, Math.abs(value - playOffTarget))));

        List<String> order = players.stream().map(Player::name).sorted((a, b) -> {
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

        int rounds = stage.roundNumber().of();
        int currentRound = stage.roundNumber().number();

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

    private Map<String, Map<Integer, Integer>> totalPoints(StageSet set, Stream<Answer> allAnswers) {
        Map<String, Map<Integer, Integer>> result = new HashMap<>();
        allAnswers.forEach(answer ->
                set.roundInit(answer.round()).map(GameStage.RoundInit::roundMode).ifPresent(mode -> {
                            var players = result.computeIfAbsent(answer.player(), _ -> new HashMap<>());
                            players.put(answer.round(), players.getOrDefault(answer.round(), 0) + answer.points());
                        }
                )
        );
        return result;
    }
}
