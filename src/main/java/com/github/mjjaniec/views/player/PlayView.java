package com.github.mjjaniec.views.player;

import com.github.mjjaniec.model.Player;
import com.github.mjjaniec.services.BroadcastAttach;
import com.github.mjjaniec.services.GameService;
import com.github.mjjaniec.util.LocalStorage;
import com.github.mjjaniec.util.Palette;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.util.Optional;


@Route(value = "play", layout = PlayerView.class)
public class PlayView extends VerticalLayout implements PlayerRoute {


    private final GameService gameService;
    private final BroadcastAttach broadcastAttach;
    private final Span caption;
    private final Button theButton;
    private Player player;

    public PlayView(GameService gameService, BroadcastAttach broadcastAttach) {
        this.gameService = gameService;
        this.broadcastAttach = broadcastAttach;
        setSpacing(true);
        setPadding(true);
        getThemeList().add("spacing-l");

        caption = new Span();
        caption.getStyle().setFontSize("10vw").setColor(Palette.WHITE);
        theButton = new Button(caption);
        theButton.setSizeFull();
        theButton.addClassName("magic-button");
        theButton.addClickListener(event ->
                Optional.ofNullable(player).ifPresent(gameService::raise));
        add(theButton);

        setSizeFull();
    }

    private void refresh() {
        gameService.stage().asPiece()
                .filter(ignored -> player != null)
                .ifPresent(piece -> {
                    theButton.setEnabled(piece.getCurrentResponder() == null);
                    theButton.getStyle().remove("background-color");
                    theButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);

                    if (piece.getCurrentResponder() == null) {
                        caption.setText("Odpowiadam");
                        theButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                    } else if (piece.getCurrentResponder().equals(player.name())) {
                        caption.setText("Odpowiadasz");
                        theButton.getStyle().setBackgroundColor(Palette.GREEN);
                    } else if (piece.getFailedResponders().contains(player.name())) {
                        if (gameService.getCurrentPlayerPoints(player) > 0) {
                            caption.setText("coś tam wiesz");
                            theButton.getStyle().setBackground(Palette.AMBER);
                        } else {
                            caption.setText("psipau");
                            theButton.getStyle().setBackground(Palette.RED);
                        }
                    } else {
                        caption.setText("aj");
                    }
                });
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        broadcastAttach.attachPlay(attachEvent.getUI(), this::refresh);
        LocalStorage.readPlayer(ui).thenAccept(playerOpt -> playerOpt.ifPresent(player -> {
            this.player = player;
            refresh();
        }));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcastAttach.detachPlay(detachEvent.getUI());
        super.onDetach(detachEvent);
    }


}
