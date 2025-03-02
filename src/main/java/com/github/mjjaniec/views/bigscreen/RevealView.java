package com.github.mjjaniec.views.bigscreen;

import com.github.mjjaniec.components.UserBadge;
import com.github.mjjaniec.model.Player;
import com.github.mjjaniec.services.BroadcastAttach;
import com.github.mjjaniec.services.GameService;
import com.github.mjjaniec.services.TestDataProvider;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "reveal", layout = BigScreenView.class)
public class RevealView extends VerticalLayout implements BigScreenRoute {

    private final GameService gameService;
    private final BroadcastAttach broadcastAttach;
    private final Div slackersContainer = new Div();

    public RevealView(GameService gameService, TestDataProvider testDataProvider, BroadcastAttach broadcastAttach) {
        this.gameService = gameService;
        this.broadcastAttach = broadcastAttach;
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

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        broadcastAttach.attachSlackersList(attachEvent.getUI(), this::refreshSlackers);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcastAttach.detachSlackersList(detachEvent.getUI());
        super.onDetach(detachEvent);
    }

    private void refreshSlackers() {
        slackersContainer.removeAll();
        slackersContainer.add(new H4("czekamy na"));
        gameService.getSlackers().forEach(player -> slackersContainer.add(new UserBadge(player.name(), true, true)));
    }
}
