package com.github.mjjaniec.views.player;

import com.github.mjjaniec.model.Player;
import com.github.mjjaniec.services.GameService;
import com.github.mjjaniec.util.LocalStorage;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;


@Route(value = "play-off", layout = PlayerView.class)
public class PlayOffView extends VerticalLayout implements PlayerRoute {

    private Player player;

    public PlayOffView(GameService gameService) {
        IntegerField field = new IntegerField("Ile nut?");
        field.setWidthFull();

        Button submit = new Button("Zatwierdź!");
        submit.setEnabled(false);
        submit.addClickListener(event -> {
            if (player != null && field.getValue() != null) {
                field.setEnabled(false);
                submit.setEnabled(false);
                add(new H4("poczekaj na pozostałych graczy"));
                add(new H2("\uD83E\uDD71 \uD83D\uDCA4 \uD83D\uDCA4"));

                gameService.savePlayOff(player, field.getValue());
            }
        });

        field.setValueChangeMode(ValueChangeMode.EAGER);
        field.addInputListener(event -> submit.setEnabled(field.getValue() != null));

        setSpacing(true);
        setPadding(true);

        setSizeFull();
        setAlignItems(Alignment.CENTER);

        add(field);
        add(submit);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        LocalStorage.readPlayer(attachEvent.getUI())
                .thenAccept(playerOpt -> playerOpt.ifPresent(player -> this.player = player));
    }
}
