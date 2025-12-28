package com.github.mjjaniec.lmq.views.player;

import com.github.mjjaniec.lmq.model.Player;
import com.github.mjjaniec.lmq.services.BroadcastAttach;
import com.github.mjjaniec.lmq.services.GameService;
import com.github.mjjaniec.lmq.util.Palette;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.jspecify.annotations.Nullable;

import java.util.Optional;


@Route(value = "play", layout = PlayerView.class)
public class PlayView extends VerticalLayout implements PlayerRoute {


    private final GameService gameService;
    private final BroadcastAttach broadcastAttach;
    private final Span caption;
    private final Button theButton;
    private @Nullable Player player;

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
        theButton.addClickListener(_ ->
                Optional.ofNullable(player).ifPresent(gameService::raise));
        add(theButton);

        setSizeFull();
    }

    private void refresh() {
        gameService.pieceStage()
                .filter(_ -> player != null)
                .ifPresent(piece -> {
                    theButton.setEnabled(piece.getCurrentResponder() == null);
                    theButton.getStyle().remove("background-color");
                    theButton.setEnabled(false);

                    if (piece.getFailedResponders().contains(player.name())) {
                        if (gameService.getCurrentPlayerPoints(player) > 0) {
                            caption.setText("Fifty fifty!");
                            theButton.getStyle().setBackgroundColor(Palette.AMBER);
                        } else {
                            caption.setText("Pudło!");
                            theButton.getStyle().setBackgroundColor(Palette.RED);
                        }
                    } else if (piece.getCurrentResponder() == null) {
                        caption.setText("Odpowiadam");
                        theButton.setEnabled(true);
                        theButton.getStyle().setBackgroundColor(Palette.BLUE);
                    } else if (piece.getCurrentResponder().equals(player.name())) {
                        caption.setText("Odpowiadasz");
                        theButton.getStyle().setBackgroundColor(Palette.GREEN);
                    } else {
                        caption.setText("Nie akwtywne");
                        theButton.getStyle().setBackgroundColor(Palette.GRAY);
                    }
                });
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        broadcastAttach.attachPlay(attachEvent.getUI(), this::refresh);
        forPlayer(ui, player -> {
            this.player = player;
            refresh();
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcastAttach.detachPlay(detachEvent.getUI());
        super.onDetach(detachEvent);
    }


}
