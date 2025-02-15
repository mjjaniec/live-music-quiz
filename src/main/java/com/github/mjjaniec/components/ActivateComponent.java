package com.github.mjjaniec.components;

import com.github.mjjaniec.model.GameStage;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import java.util.function.Consumer;

public class ActivateComponent extends Div {
    private final GameStage stage;
    private final Consumer<GameStage> onActivate;

    public ActivateComponent(GameStage stage, boolean active, Consumer<GameStage> onActivate) {
        this.stage = stage;
        this.onActivate = onActivate;
        setActive(active);
    }

    public void setActive(boolean active) {
        removeAll();

        if (active) {
            Span badge = new Span("Aktywne ✨⭐\uD83D\uDD25");
            badge.getElement().getThemeList().add("badge success pill");
            add(badge);
        } else {
            Button button = new Button("Aktywuj", event -> onActivate.accept(stage));
            button.addThemeVariants(ButtonVariant.LUMO_SMALL);
            add(button);
        }
    }
}
