package com.github.mjjaniec.components;

import com.github.mjjaniec.model.Results;
import com.github.mjjaniec.util.Palette;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.stream.Stream;

public class PodiumComponent extends HorizontalLayout {
    public PodiumComponent(Results results, int showFrom) {
        setSizeFull();
        add(section(2, Results.Award.SECOND, results, showFrom));
        add(section(1, Results.Award.FIRST, results, showFrom));
        add(section(3, Results.Award.THIRD, results, showFrom));
    }


    private Stream<String> findPlayers(Results results, Results.Award award) {
        return results.rows().stream().filter(r -> r.award().map(a -> a == award).orElse(false)).map(Results.Row::player);
    }

    private Component section(int position, Results.Award award, Results results, int showFrom) {
        VerticalLayout result = new VerticalLayout();
        result.setSpacing(false);
        result.setPadding(false);
        result.setWidthFull();

        VerticalLayout caption = new VerticalLayout();
        Div filler = new Div();
        filler.setWidthFull();
        filler.setHeight(switch (position) {
            case 1 -> "20vh";
            case 2 -> "12vh";
            case 3 -> "8vh";
            default -> "0";
        });
        caption.add(filler);
        caption.add(new H1(award.symbol));
        caption.add(new H1(String.valueOf(position)));
        caption.getStyle().setBackgroundColor(Palette.DARKER);

        if (position >= showFrom) {
            findPlayers(results, award)
                    .forEach(player -> result.add(new UserBadge(player, false, true)));
        }

        result.add(caption);
        return result;
    }


}
