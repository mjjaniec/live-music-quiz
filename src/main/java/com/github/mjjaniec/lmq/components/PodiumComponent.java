package com.github.mjjaniec.lmq.components;

import com.github.mjjaniec.lmq.services.Results;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Style;

import java.util.List;

import static com.github.mjjaniec.lmq.util.TestId.testId;

public class PodiumComponent extends VerticalLayout {
    public PodiumComponent(Results results, int showFrom) {
        setSizeFull();
        setSpacing(false);
        setPadding(true);
        testId(this, "big-screen/podium");
        HorizontalLayout podium = new HorizontalLayout();
        podium.setSizeFull();
        podium.setPadding(false);
        podium.setSpacing(false);
        podium.add(section(2, Results.Award.SECOND, results, showFrom));
        podium.add(section(1, Results.Award.FIRST, results, showFrom));
        podium.add(section(3, Results.Award.THIRD, results, showFrom));
        podium.setAlignItems(Alignment.END);
        add(podium);
        add(base(results));
    }

    private Span baseSpan(String text) {
        Span result = new Span(text);
        result.getStyle().setFontSize("3vh");
        return result;
    }

    private Component base(Results results) {
        var award = Results.Award.PLAY_OFF;
        HorizontalLayout result = new HorizontalLayout();
        result.setWidthFull();
        result.addClassName(award.style);
        result.setPadding(true);
        result.add(baseSpan(award.symbol));
        HorizontalLayout playOffs = new HorizontalLayout();
        playOffs.setWidthFull();
        testId(playOffs, "big-screen/podium/playoffs");
        playOffs.setJustifyContentMode(JustifyContentMode.EVENLY);
        findPlayers(results, award).forEach(row -> playOffs.add(baseSpan(row.player() + " (" + row.playOff() + ")")));
        result.add(playOffs);
        result.add(baseSpan(Results.Award.PLAY_OFF.symbol));
        return result;
    }


    private List<Results.Row> findPlayers(Results results, Results.Award award) {
        return results.rows().stream().filter(r -> r.award().map(a -> a == award).orElse(false)).toList();
    }

    private Component section(int position, Results.Award award, Results results, int showFrom) {
        VerticalLayout result = new VerticalLayout();
        result.setSpacing(false);
        result.setPadding(false);
        result.setWidthFull();

        VerticalLayout badges = new VerticalLayout();
        badges.setSpacing(false);
        badges.setPadding(false);
        badges.setWidthFull();
        badges.setAlignItems(Alignment.CENTER);

        testId(badges, "big-screen/podium/segment-" + position);

        VerticalLayout segment = new VerticalLayout();


        segment.setAlignItems(Alignment.CENTER);
        Div filler = new Div();
        filler.setWidthFull();
        filler.setHeight(switch (position) {
            case 1 -> "12vh";
            case 2 -> "4vh";
            default -> "0";
        });
        segment.add(filler);

        HorizontalLayout caption = new HorizontalLayout();
        caption.setWidthFull();
        caption.add(new H1(award.symbol));
        Div number = new Div(String.valueOf(position));
        number.getStyle().setFontSize("8vh").setFontWeight(Style.FontWeight.BOLD).setLineHeight("1.5");
        number.setClassName("pt-mono-regular");
        caption.add(number);
        caption.add(new H1(award.symbol));
        caption.setAlignItems(Alignment.BASELINE);
        caption.setJustifyContentMode(JustifyContentMode.EVENLY);
        segment.add(caption);
        segment.addClassName(award.style);

        if (position >= showFrom) {
            List<Results.Row> players = findPlayers(results, award);
            if (!players.isEmpty()) {
                Results.Row first = players.getFirst();
                result.add(baseSpan(first.total() + " / " + first.playOff()));
            }
            players.forEach(player -> {
                UserBadge badge = new UserBadge(player.player(), false, true);
                badge.getStyle().setFontSize("6vh");
                badges.add(badge);
            });
        }

        result.add(badges, segment);
        return result;
    }


}
