package com.github.mjjaniec.lmq.views.player;

import com.github.mjjaniec.lmq.model.Player;
import com.github.mjjaniec.lmq.services.BroadcastAttach;
import com.github.mjjaniec.lmq.services.GameService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import org.jspecify.annotations.Nullable;
import static com.github.mjjaniec.lmq.util.TestId.testId;


@Route(value = "play-off", layout = PlayerView.class)
public class PlayOffView extends VerticalLayout implements PlayerRoute {

    private final GameService gameService;
    private final BroadcastAttach broadcastAttach;
    @Nullable
    private Player player;

    public PlayOffView(GameService gameService, BroadcastAttach broadcastAttach) {
        this.gameService = gameService;
        this.broadcastAttach = broadcastAttach;


        setSpacing(true);
        setPadding(true);

        setSizeFull();
        setAlignItems(Alignment.CENTER);
    }

    private void refreshPlayOff() {
        gameService.playOffStage().ifPresent(playOff -> {
            removeAll();
            if (playOff.isPerformed()) {
                IntegerField field = testId(new IntegerField("Ile nut?"), "player/play-off/value");
                field.setWidthFull();

                Button submit = testId(new Button("Zatwierdź!"), "player/play-off/submit");
                submit.setEnabled(false);
                submit.addClickListener(_ -> {
                    if (player != null && field.getValue() != null) {
                        field.setEnabled(false);
                        submit.setEnabled(false);
                        add(new H4("poczekaj na pozostałych graczy"));
                        add(new H2("\uD83E\uDD71 \uD83D\uDCA4 \uD83D\uDCA4"));

                        gameService.savePlayOff(player, field.getValue());
                    }
                });

                field.setValueChangeMode(ValueChangeMode.EAGER);
                field.addInputListener(_ -> submit.setEnabled(field.getValue() != null));
                add(field);
                add(submit);
            } else {
                H1 headphones = new H1("\uD83C\uDFA7");
                headphones.addClassName("pulse");
                add(headphones);
            }
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcastAttach.detachPlayOff(detachEvent.getUI());
        super.onDetach(detachEvent);
    }


    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        forPlayer(attachEvent.getUI(), player -> this.player = player);
        broadcastAttach.attachPlayOff(attachEvent.getUI(), this::refreshPlayOff);
        refreshPlayOff();
    }
}
