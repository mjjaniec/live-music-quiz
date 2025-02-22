package com.github.mjjaniec.views.bigscreen;

import com.github.mjjaniec.components.UserBadge;
import com.github.mjjaniec.model.GameStage;
import com.github.mjjaniec.model.Player;
import com.github.mjjaniec.services.BroadcastAttach;
import com.github.mjjaniec.services.GameService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;

import java.util.List;

@Route(value = "invite", layout = BigScreenView.class)
public class InviteView extends HorizontalLayout implements BigScreenRoute {
    private final GameService gameService;
    private final BroadcastAttach broadcastAttach;
    private final Div playersContainer = new Div();


    public InviteView(GameService gameService, BroadcastAttach broadcastAttach) {
        this.gameService = gameService;
        this.broadcastAttach = broadcastAttach;
        setSizeFull();
        setSpacing(false);

        boolean isProduction = VaadinService.getCurrent().getDeploymentConfiguration().isProductionMode();
        String dataMatrix = isProduction ? "link-data-matrix.svg" : "local-link-data-matrix.svg";
        String url = isProduction ? "https://live-music-quiz.onrender.com" : "http://192.168.31.27:8080";

        VerticalLayout invitation = new VerticalLayout();
        invitation.setAlignItems(Alignment.CENTER);
        invitation.setHeightFull();
        Image dm = new Image("themes/live-music-quiz/" + dataMatrix, "link");
        dm.setHeightFull();
        H1 h = new H1(url);
        h.getStyle().setMarginTop("1em");
        invitation.add(h);
        invitation.add(dm);

        VerticalLayout players = new VerticalLayout();

        players.add(playersContainer);
        players.setHeightFull();
        players.setJustifyContentMode(JustifyContentMode.CENTER);
        players.setAlignItems(Alignment.CENTER);
        refreshPlayers();

        add(invitation, players);
    }

    private void refreshPlayers() {
        playersContainer.removeAll();
        List<Player> plaersList = gameService.getPlayers();

        plaersList.forEach(user -> playersContainer.add(new UserBadge(user.name(), plaersList.size() > 10, true)));
        playersContainer.add(new H2(plaersList.isEmpty() ? "Czekamy na graczy" : "grają z nami! (" + plaersList.size() + " os)"));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        broadcastAttach.attachPlayerList(attachEvent.getUI(), this::refreshPlayers);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        broadcastAttach.detachPlayerList(detachEvent.getUI());
    }
}
