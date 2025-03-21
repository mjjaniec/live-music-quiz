package com.github.mjjaniec.views.bigscreen;

import com.github.mjjaniec.model.Player;
import com.github.mjjaniec.services.GameService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.router.Route;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Route(value = "round-summary", layout = BigScreenView.class)
public class RoundSummaryView extends VerticalLayout implements BigScreenRoute {

    public RoundSummaryView(GameService gameService) {
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        Grid<Player> playersGrid = new Grid<>(Player.class, false);
        playersGrid.setHeightFull();
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
        int rounds = (int) gameService.stageSet().topLevelStages().stream().filter(stage -> stage.asRoundInit().isPresent()).count();
        int currentRound = gameService.stage().asRoundSummary().map(s -> s.roundNumber().number()).orElse(0);

        playersGrid.addColumn(new ComponentRenderer<>((SerializableFunction<Player, Component>) player -> {
            if (!order.isEmpty() && order.get(0).name().equals(player.name())) {
                return new H1("\uD83E\uDDC5");
            } else if (order.size() >= 2 && order.get(1).name().equals(player.name())) {
                return new H2("\uD83E\uDDC4");
            } else if (order.size() >= 3 && order.get(2).name().equals(player.name())) {
                return new H3("\uD83E\uDD54");
            }
            return new Span();
        })).setHeader("Nagroda");

        playersGrid.addColumn(new ComponentRenderer<>((SerializableFunction<Player, Component>) player -> new H4(player.name())))
                .setHeader("Ksywka");

        for (int i = 1; i <= rounds; ++i) {
            int finalI = i;
            playersGrid.addColumn(new ComponentRenderer<>((SerializableFunction<Player, Component>) player -> {
                        if (finalI <= currentRound) {
                            Integer points = byRounds.getOrDefault(player.name(), Map.of()).getOrDefault(finalI, 0);
                            return new H5(String.valueOf(points));
                        } else {
                            return new Span();
                        }
                    }))
                    .setHeader("Runda " + finalI);
        }

        playersGrid.addColumn(new ComponentRenderer<>((SerializableFunction<Player, Component>) player ->
                        new H5(String.valueOf(altogether.getOrDefault(player.name(), 0)))))
                .setHeader("Razem");


        playersGrid.setItems(order);
        add(playersGrid);
    }
}
