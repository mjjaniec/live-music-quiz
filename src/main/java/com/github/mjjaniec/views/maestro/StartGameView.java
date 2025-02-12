package com.github.mjjaniec.views.maestro;

import com.github.mjjaniec.model.MainSet;
import com.github.mjjaniec.services.MaestroInterface;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;

import java.util.Set;
import java.util.stream.Stream;

@Route(value = "start", layout = MaestroView.class)
public class StartGameView extends VerticalLayout implements RouterLayout {

    StartGameView(MaestroInterface gameService) {
        Set<String> sets = MainSet.TheSet.sets();
        String ALL = "ALL";

        ComboBox<String> games = new ComboBox<>("Choose Game");
        games.setItems(Stream.concat(sets.stream(), Stream.of(ALL)).toList());
        Button start = new Button("Start");
        start.setEnabled(false);
        games.addValueChangeListener(event -> start.setEnabled(true));


        start.addClickListener(event -> {
            MainSet set = ALL.equals(games.getValue()) ? MainSet.TheSet : MainSet.TheSet.asSet(games.getValue());
            gameService.initGame(set);
            UI.getCurrent().navigate(DjView.class);
        });
        add(games, start);
    }
}
