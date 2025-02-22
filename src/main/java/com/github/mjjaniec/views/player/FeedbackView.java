package com.github.mjjaniec.views.player;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;

@Route(value = "feedback", layout = PlayerView.class)
public class FeedbackView extends VerticalLayout implements PlayerRoute {

    public FeedbackView() {
        setSpacing(false);
        setSizeFull();

        var input = new TextArea("Jak się podobało, jakieś karmienie zwrotne?", "Będzie mi miło jeśli napiszesz że było fajnie. Ale docenię jescze bardziej jeśli wskarzesz coś co można ulepszyć :)");
        input.setSizeFull();
        Button button = new Button("Wyślij");
        button.setWidthFull();
        button.setEnabled(false);
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        input.setValueChangeMode(ValueChangeMode.EAGER);
        input.addInputListener(event -> button.setEnabled(!input.getValue().isBlank()));
        add(input, button);
        setAlignItems(Alignment.CENTER);


        button.addClickListener(event -> {
            removeAll();
            add(new H3("Diękówa!"));
            H1 heart = new H1("❤️");
            heart.addClassName("pulse");
            add(heart);
            setJustifyContentMode(JustifyContentMode.EVENLY);
        });
    }
}
