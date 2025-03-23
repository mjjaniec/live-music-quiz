package com.github.mjjaniec.components;

import com.github.mjjaniec.model.Player;
import com.github.mjjaniec.services.GameService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.function.SerializableFunction;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ResultsTable extends Grid<Player> {

    public ResultsTable(GameService gameService, int showFrom) {
        setSizeFull();
        Map<String, Map<Integer, Integer>> byRounds = gameService.totalPoints();
        Map<String, Integer> altogether = byRounds.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().values().stream().mapToInt(x -> x).sum()
        ));
        List<Player> order = gameService.getPlayers().stream().sorted((a, b) -> {
            int aPoints = altogether.getOrDefault(a.name(), 0);
            int bPoints = altogether.getOrDefault(b.name(), 0);
            if (aPoints != bPoints) {
                return bPoints - aPoints;
            } else {
                return a.name().compareTo(b.name());
            }
        }).toList();

        int start = 1;
        int count = 1;
        Map<Player, Integer> positions = new HashMap<>();
        Player previous = order.getFirst();
        positions.put(previous, start);
        for (Player p : order) {
            if (p == previous) continue;
            if (Objects.equals(altogether.getOrDefault(p.name(), 0), altogether.getOrDefault(previous.name(), 0))) {
                positions.put(p, positions.get(previous));
                count += 1;
            } else {
                start += count;
                positions.put(p, start);
                count = 1;
            }
            previous = p;
        }

        final Set<Player> bestPlayOffs = new HashSet<>();
        bestPlayOffs.add(new Player("Kasia"));

        int rounds = (int) gameService.stageSet().topLevelStages().stream().filter(stage -> stage.asRoundInit().isPresent()).count();
        int currentRound = gameService.stage().asRoundSummary().map(s -> s.roundNumber().number()).orElse(rounds);

        addColumn("Pozycja", player -> text(String.valueOf(positions.get(player))));
        addColumn("Token", player -> switch (positions.get(player)) {
            case 1 -> text("\uD83E\uDDC5");
            case 2 -> text("\uD83E\uDDC4");
            case 3 -> text("\uD83E\uDD54");
            default -> text(bestPlayOffs.contains(player) ? "\uD83C\uDF36" : "");
        });
        addColumn("Ksywka", player -> boldText(player.name()));

        for (int i = 1; i <= rounds; ++i) {
            Function<Player, Component> generator;
            if (i <= currentRound) {
                int finalI = i;
                generator = player -> text(String.valueOf(byRounds.getOrDefault(player.name(), Map.of()).getOrDefault(finalI, 0)));
            } else {
                generator = ignored -> new Span();
            }
            addColumn("Runda " + i, generator);
        }


        addColumn("Dogrywka", player -> text(currentRound == rounds ? String.valueOf(gameService.getPlayOff(player)) : ""));
        addColumn("Razem", player -> boldText(String.valueOf(altogether.getOrDefault(player.name(), 0))));


        setPartNameGenerator(player -> {
            int position = positions.get(player);
            String maybeHidden = (position < showFrom ? " hidden" : "");
            return switch (position) {
                case 1 -> "gold" + maybeHidden;
                case 2 -> "silver" + maybeHidden;
                case 3 -> "bronze" + maybeHidden;
                default -> (bestPlayOffs.contains(player) ? "rust" : "") + maybeHidden;
            };
        });

        setItems(order);
    }

    private void addColumn(String caption, Function<Player, Component> renderer) {
        addColumn(new ComponentRenderer<>((SerializableFunction<Player, Component>) renderer::apply)).setHeader(caption);
    }

    private Component text(String text) {
        H5 result = new H5(text);
        result.getStyle().setColor("inherit");
        return result;
    }

    private Component boldText(String text) {
        H4 result = new H4(text);
        result.getStyle().setColor("inherit");
        return result;
    }
}
