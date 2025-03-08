package com.github.mjjaniec.views.player;

import com.github.mjjaniec.components.NotesAnimation;
import com.github.mjjaniec.components.UserBadge;
import com.github.mjjaniec.services.BroadcastAttach;
import com.github.mjjaniec.services.GameService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;


@Route(value = "play", layout = PlayerView.class)
public class PlayView extends VerticalLayout implements PlayerRoute {


    private final GameService gameService;
    private final BroadcastAttach broadcastAttach;

    public PlayView(GameService gameService, BroadcastAttach broadcastAttach) {
        this.gameService = gameService;
        this.broadcastAttach = broadcastAttach;
        setSpacing(true);
        setPadding(true);
        getThemeList().add("spacing-l");

        setSizeFull();
    }

    private void refresh() {
        gameService.stage().asPiece().ifPresent(piece -> {
            removeAll();

            if (piece.getCurrentResponder() == null) {
                add(new Div());
                add(new H1("Słuchaj słuchaj jaj jaj"));
                add(new VerticalLayout());

                Component animation = new NotesAnimation();
                animation.getStyle().setMaxWidth("8em");
                add(animation);
            } else {
                add(new Div());
                add(new H1("Odpowiada:"));
                add(new VerticalLayout());
                add(new UserBadge(piece.getCurrentResponder(), false, true));
            }
        });
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        broadcastAttach.attachPlay(attachEvent.getUI(), this::refresh);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcastAttach.detachPlay(detachEvent.getUI());
        super.onDetach(detachEvent);
    }


}
