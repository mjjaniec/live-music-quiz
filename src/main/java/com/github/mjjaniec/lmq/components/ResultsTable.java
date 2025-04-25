package com.github.mjjaniec.lmq.components;

import com.github.mjjaniec.lmq.services.Results;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.function.SerializableFunction;

import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResultsTable extends Grid<Results.Row> {

    public ResultsTable(Results results, int showFrom) {
        setSizeFull();

        addColumn("Pozycja", row -> text(String.valueOf(row.position())));
        addColumn("Nagroda", row -> {
            H3 res = new H3(row.award().map(a -> a.symbol).orElse(""));
            res.getStyle().setLineHeight("2.2rem");
            return res;
        });
        addColumn("Ksywka", row -> boldText(row.player()));

        for (int i = 1; i <= results.rounds(); ++i) {
            Function<Results.Row, Component> generator;
            if (i <= results.currentRound()) {
                int finalI = i;
                generator = row -> text(String.valueOf(row.rounds().getOrDefault(finalI, 0)));
            } else {
                generator = ignored -> new Span();
            }
            addColumn("Runda " + i, generator);
        }


        addColumn("Dogrywka", row -> text(results.rounds() == results.currentRound() ? String.valueOf(row.playOff()) : ""));
        addColumn("Razem", row -> boldText(String.valueOf(row.total())));


        setPartNameGenerator(row ->
            Stream.concat(
                    row.award().filter(a -> a != Results.Award.PLAY_OFF || showFrom <= 4).map(a -> a.style).stream(),
                    Stream.of("hidden").filter(ignored -> row.position() < showFrom)
            ).collect(Collectors.joining(" "))
        );

        setItems(results.rows());
    }

    private void addColumn(String caption, Function<Results.Row, Component> renderer) {
        addColumn(new ComponentRenderer<>((SerializableFunction<Results.Row, Component>) renderer::apply)).setHeader(caption);
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
