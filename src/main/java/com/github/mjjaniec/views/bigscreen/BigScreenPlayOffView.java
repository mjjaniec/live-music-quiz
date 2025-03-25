package com.github.mjjaniec.views.bigscreen;

import com.github.mjjaniec.components.NotesAnimation;
import com.github.mjjaniec.components.UserBadge;
import com.github.mjjaniec.services.BroadcastAttach;
import com.github.mjjaniec.services.GameService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;


@Route(value = "play-off", layout = BigScreenView.class)
public class BigScreenPlayOffView extends VerticalLayout implements BigScreenRoute {

    private final GameService gameService;
    private final BroadcastAttach broadcastAttach;
    private final Div slackersContainer = new Div();

    public BigScreenPlayOffView(GameService gameService, BroadcastAttach broadcastAttach) {
        this.gameService = gameService;
        this.broadcastAttach = broadcastAttach;
        setSpacing(true);
        setPadding(true);
        getThemeList().add("spacing-l");

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        refreshPlayOff();
    }

    void refreshPlayOff() {
        removeAll();
        gameService.stage().asPlayOff().ifPresent(playOff -> {
            if (!playOff.isPerformed()) {
                add(new Div());
                add(new H1("Słuchaj i licz!"));
                add(new VerticalLayout());

                Component animation = new NotesAnimation();
                animation.getStyle().setMaxWidth("8em");
                add(animation);
                add(new VerticalLayout());
                add(new H1("∬(∇×B-µ(J+ε∙∂E/∂t))dS"));
            } else {
                add(new H1("No i ile?"));
                add(new H1("\uD83E\uDD28\uD83E\uDD14\uD83E\uDD2F"));
                add(slackersContainer);
                refreshSlackers();
            }
        });
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        broadcastAttach.attachSlackersList(attachEvent.getUI(), this::refreshSlackers);
        broadcastAttach.attachPlayOff(attachEvent.getUI(), this::refreshPlayOff);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcastAttach.detachSlackersList(detachEvent.getUI());
        broadcastAttach.detachPlayOff(detachEvent.getUI());
        super.onDetach(detachEvent);
    }

    private void refreshSlackers() {
        slackersContainer.removeAll();
        slackersContainer.add(new H4("czekamy na"));
        gameService.getSlackers().forEach(player -> slackersContainer.add(new UserBadge(player.name(), true, true)));
    }
}
