package com.github.mjjaniec.views.maestro;

import com.github.mjjaniec.model.MainSet;
import com.github.mjjaniec.model.SpreadsheetLoader;
import com.github.mjjaniec.services.MaestroInterface;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;
import java.util.stream.Stream;

@Route(value = "start", layout = MaestroView.class)
public class StartGameView extends VerticalLayout implements RouterLayout {

    StartGameView(MaestroInterface gameService, SpreadsheetLoader loader) {
        try {
            MainSet mainSet = loader.loadMainSet();
            loader.loadPlayOffs();

            Set<String> sets = mainSet.sets();
            String ALL = "ALL";

            ComboBox<String> games = new ComboBox<>("Choose Game");
            games.setItems(Stream.concat(sets.stream(), Stream.of(ALL)).toList());
            Button start = new Button("Start");
            start.setEnabled(false);
            games.addValueChangeListener(event -> start.setEnabled(true));


            start.addClickListener(event -> {
                MainSet set = ALL.equals(games.getValue()) ? mainSet :mainSet.asSet(games.getValue());
                gameService.initGame(set.shuffle());
                UI.getCurrent().navigate(DjView.class);
            });
            add(games, start);
        } catch (Throwable throwable) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            throwable.printStackTrace(printWriter);
            Paragraph p = new Paragraph(stringWriter.toString());
            p.getStyle().setWhiteSpace(Style.WhiteSpace.PRE_WRAP);
            add(p);
        }
    }
}
