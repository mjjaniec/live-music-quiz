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
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route(value = "play", layout = BigScreenView.class)
public class BigScreenPlayView extends VerticalLayout implements BigScreenRoute {

    private final GameService gameService;
    private final BroadcastAttach broadcastAttach;

    public BigScreenPlayView(GameService gameService, BroadcastAttach broadcastAttach) {
        this.gameService = gameService;
        this.broadcastAttach = broadcastAttach;
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setSpacing(true);
        setPadding(true);
        getThemeList().add("spacing-l");
    }

    private void refreshState() {
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
        broadcastAttach.attachPlay(attachEvent.getUI(), this::refreshState);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcastAttach.detachPlay(detachEvent.getUI());
        super.onDetach(detachEvent);
    }
}
