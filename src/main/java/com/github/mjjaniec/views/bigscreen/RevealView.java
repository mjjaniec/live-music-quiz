package com.github.mjjaniec.views.bigscreen;

import com.github.mjjaniec.components.UserBadge;
import com.github.mjjaniec.model.Player;
import com.github.mjjaniec.services.GameService;
import com.github.mjjaniec.services.TestDataProvider;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "reveal", layout = BigScreenView.class)
public class RevealView extends VerticalLayout implements BigScreenRoute {

    Div slackersContainer = new Div();

    public RevealView(GameService gameService, TestDataProvider testDataProvider) {
        setSpacing(false);
        setPadding(true);

        gameService.stage().asPiece().or(testDataProvider::piece)
                        .ifPresent(piece -> {
                            add(new VerticalLayout());
                            add(new H4("wykonawca"));
                            add(new H1(piece.piece.artist()));
                            add(new VerticalLayout());
                            add(new H4("tytuł"));
                            add(new H1(piece.piece.title()));
                        });

        setSizeFull();
        slackersContainer.setWidthFull();
        setAlignItems(Alignment.CENTER);
        add(new Div());
        add(slackersContainer);
        add(new VerticalLayout());

        refreshSlackers();
    }

    private void refreshSlackers() {
        slackersContainer.removeAll();
        List<Player> slackers = List.of(new Player("Makumba"), new Player("Kasie"), new Player("ktoś tam"), new Player("Antylopa"));
        slackersContainer.add(new H4("czekamy na"));
        slackers.forEach(player -> slackersContainer.add(new UserBadge(player.name(), true, true)));
    }
}
