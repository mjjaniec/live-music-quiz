package com.github.mjjaniec.lmq.components;

import com.github.mjjaniec.lmq.services.Results;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.function.SerializableFunction;

import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.mjjaniec.lmq.util.TestId.testId;

public class ResultsTable extends Grid<Results.Row> {

    public ResultsTable(Results results, int showFrom, boolean showAwards) {
        setSizeFull();

        boolean showPlayoff = showFrom <= 4;
        addThemeVariants(GridVariant.LUMO_NO_ROW_BORDERS);
        addThemeVariants(GridVariant.LUMO_NO_BORDER);
        addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        addColumn("", 0, row -> new Span());
        addColumn("Pozycja", 0, row -> {
            var res = text(String.valueOf(row.position()));
            res.getStyle().setLineHeight("2.2rem");
            return testId(res, "big-screen/results/position-" + row.position());
        });
        addColumn("Nagroda", 0, row -> {
            H3 res = new H3(row.award().filter(_ -> showPlayoff && showAwards).map(a -> a.symbol).orElse(""));
            res.getStyle().setLineHeight("2.2rem");
            return testId(res, "big-screen/results/prize-" + row.position());
        });
        addColumn("Ksywka", 5, row -> testId(
                boldText(row.player()),
                "big-screen/results/nickname-" + row.position()));

        for (int i = 1; i <= results.rounds(); ++i) {
            Function<Results.Row, Component> generator;
            if (i <= results.currentRound()) {
                int finalI = i;
                generator = row -> testId(
                        text(String.valueOf(row.rounds().getOrDefault(finalI, 0))),
                        "big-screen/results/round-" + finalI + "-" + row.position());
            } else {
                generator = ignored -> new Span();
            }
            addColumn("Runda " + i, 0, generator);
        }


        addColumn("Dogrywka", 1, row -> testId(
                text(results.rounds() == results.currentRound() ? row.playOff() + " / " + results.targetPlayOff() : ""),
                "big-screen/results/playoff-" + row.position())
        );
        addColumn("Razem", 1, row -> testId(
                boldText(String.valueOf(row.total())),
                "big-screen/results/total-" + row.position()));


        setPartNameGenerator(row ->
                Stream.concat(
                        row.award().filter(_ -> showPlayoff && showAwards).map(a -> a.style).stream(),
                        Stream.of("invisible").filter(ignored -> row.position() < showFrom)
                ).collect(Collectors.joining(" "))
        );

        setAllRowsVisible(true);
        setItems(results.rows());
    }

    private void addColumn(String caption, int flexGrow, Function<Results.Row, Component> renderer) {
        addColumn(new ComponentRenderer<>((SerializableFunction<Results.Row, Component>) renderer::apply)).setHeader(caption).setFlexGrow(flexGrow);
    }

    private Component text(String text) {
        H4 result = new H4(text);
        result.getStyle().setColor("inherit");
        return result;
    }

    private Component boldText(String text) {
        H4 result = new H4(text);
        result.getStyle().setColor("inherit");
        return result;
    }
}
