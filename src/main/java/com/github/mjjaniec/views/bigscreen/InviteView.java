package com.github.mjjaniec.views.bigscreen;

import com.github.mjjaniec.model.Player;
import com.github.mjjaniec.services.BroadcastAttach;
import com.github.mjjaniec.services.PlayerStore;
import com.github.mjjaniec.util.R;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;

import java.util.List;

@Route(value = R.BigScreen.Invite.PATH, layout = BigScreenView.class)
public class InviteView extends HorizontalLayout {
    private final PlayerStore playerService;
    private final BroadcastAttach broadcastAttach;
    private final Div playersContainer = new Div();


    public InviteView(PlayerStore playerService, BroadcastAttach broadcastAttach) {
        this.playerService = playerService;
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

    private Component userBadge(String user, boolean small) {
        Span badge = new Span(user);
        String fontSize = small ? "2em" : "2.5em" ;
        String margin = small ?  "0.4em" : "0.5em";
        badge.getStyle().setFontSize(fontSize);
        badge.getStyle().setMarginRight(margin);
        badge.getStyle().setMarginBottom(margin);
        badge.getElement().getThemeList().add("badge success pill");
        return badge;
    }

    private void refreshPlayers() {
        playersContainer.removeAll();
        List<Player> plaersList = playerService.getPlayers();

        plaersList.forEach(user -> playersContainer.add(userBadge(user.name(), plaersList.size() > 10)));
        playersContainer.add(new H2(plaersList.isEmpty() ? "waiting for players" : "play with us!"));
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
