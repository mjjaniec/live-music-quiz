package com.github.mjjaniec.lmq.views.bigscreen;

import com.github.mjjaniec.lmq.components.NotesAnimation;
import com.github.mjjaniec.lmq.model.GameStage;
import com.github.mjjaniec.lmq.model.MainSet;
import com.github.mjjaniec.lmq.services.BroadcastAttach;
import com.github.mjjaniec.lmq.services.GameService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route(value = "listen", layout = BigScreenView.class)
public class BigScreenListenView extends VerticalLayout implements BigScreenRoute {
    private final SlackersContainer slackersContainer = new SlackersContainer();
    private final GameService gameService;
    private final BroadcastAttach broadcastAttach;
    private final H1 message = new H1();

    public BigScreenListenView(GameService gameService, BroadcastAttach broadcastAttach) {
        this.gameService = gameService;
        this.broadcastAttach = broadcastAttach;
        setSpacing(true);
        setPadding(true);
        getThemeList().add("spacing-l");

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        add(new Div());
        add(message);
        add(new VerticalLayout());

        Component animation = new NotesAnimation();
        animation.getStyle().setMaxWidth("8em");
        add(animation);

        slackersContainer.setWidthFull();
        setAlignItems(Alignment.CENTER);
        add(new Div());
        add(slackersContainer);
        add(new VerticalLayout());

        refreshSlackers();
        refreshBonusMessage();
    }

    private void refreshBonusMessage() {
        gameService.pieceStage().map(GameStage.RoundPiece::isBonus).ifPresent(bonus -> {
            if (bonus) {
                message.setText("Słuchaj i zgarnij BONUS! \uD83D\uDC8E");
            } else {
                message.setText("Słuchaj, słuchaj jaj jaj");
            }
        });
    }

    private void refreshSlackers() {
        slackersContainer.refresh(gameService.getSlackers());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        broadcastAttach.attachSlackersList(attachEvent.getUI(), this::refreshSlackers);
        broadcastAttach.attachBonusListener(attachEvent.getUI(), this::refreshBonusMessage);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcastAttach.detachBonusListener(detachEvent.getUI());
        broadcastAttach.detachSlackersList(detachEvent.getUI());
        super.onDetach(detachEvent);
    }
}
