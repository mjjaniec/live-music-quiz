package com.github.mjjaniec.views.player;

import com.github.mjjaniec.services.GameService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;


@Route(value = "play-off", layout = PlayerView.class)
public class PlayOffView extends VerticalLayout implements PlayerRoute {

    public PlayOffView(GameService gameService) {
        IntegerField field = new IntegerField("Ile nut?");
        field.setWidthFull();

        Button submit = new Button("Zatwierdź!");
        submit.setEnabled(false);
        submit.addClickListener(event -> {

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

}
