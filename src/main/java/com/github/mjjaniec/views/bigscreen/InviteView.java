package com.github.mjjaniec.views.bigscreen;

import com.github.mjjaniec.model.Player;
import com.github.mjjaniec.services.PlayerStore;
import com.github.mjjaniec.util.R;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;

import java.util.List;

@Route(value = R.BigScreen.Invite.PATH, layout = BigScreenView.class)
public class InviteView extends HorizontalLayout {
//    private List<String> users = List.of("Beata", "Elżbieta", "Jolanta", "Asia", "Eliza", "Klaudia"
//            ,            "Andrea", "Regina", "Franciszka", "Joanna", "Antonina", "Bogumiła", "Matylda", "Adriana",
//            "Jola", "Arkadiusz", "Julian", "Alex", "Kuba", "Bogumił", "Alfred", "Bartosz", "Czesław", "Olaf", "Dorian", "Mateusz", "Miron", "Radosław", "Diego", "Korneliusz"
//    );

    public InviteView(PlayerStore playerService) {
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
        Div container = new Div();

        List<Player> plaersList = playerService.getPlayers();

        plaersList.forEach(user -> container.add(userBadge(user.name(), plaersList.size() > 10)));
        container.add(new H2(plaersList.isEmpty() ? "waiting for players" : "play with us!"));
        players.add(container);
        players.setHeightFull();
        players.setJustifyContentMode(JustifyContentMode.CENTER);
        players.setAlignItems(Alignment.CENTER);

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
}
