package com.github.mjjaniec.lmq.views.player;

import com.github.mjjaniec.lmq.services.GameService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Value;

@Route(value = "feedback", layout = PlayerView.class)
public class FeedbackView extends VerticalLayout implements PlayerRoute {

    private @Value("${application.form-url}") String formUrl;

    public FeedbackView(GameService gameService) {
        setSpacing(false);
        setSizeFull();

        var input = new TextArea("Jak się podobało?", "Będzie mi miło jeśli napiszesz że było fajnie. Ale docenię jescze bardziej jeśli wskażesz coś co można ulepszyć :)");
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
            gameService.saveFeedback(input.getValue());
            removeAll();
            add(new H3("Dzięki!"));
            H1 heart = new H1("❤️");
            heart.addClassName("pulse");
            add(heart);
            setJustifyContentMode(JustifyContentMode.EVENLY);
            add(new H4(new Anchor(formUrl, "Chcę zaproponować utwór na kolejna edycję")));
        });
    }
}
