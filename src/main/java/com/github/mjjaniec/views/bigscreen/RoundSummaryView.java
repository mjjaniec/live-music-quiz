package com.github.mjjaniec.views.bigscreen;

import com.github.mjjaniec.model.Player;
import com.github.mjjaniec.services.GameService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.router.Route;

@Route(value = "round-summary", layout = BigScreenView.class)
public class RoundSummaryView extends VerticalLayout implements BigScreenRoute {

    public RoundSummaryView(GameService gameService) {
        Grid<Player> playersGrid = new Grid<>(Player.class, false);
        playersGrid.addColumn(p -> p.name).setHeader("Ksywka");
        playersGrid.addColumn(new ComponentRenderer<>((SerializableFunction<Player, Component>) player -> {
            Div result = new Div();
            Checkbox danger = new Checkbox("danger", false);
            Button bumpOut = new Button("Wyrzuć", event -> gameService.removePlayer(player));
            bumpOut.addThemeVariants(ButtonVariant.LUMO_ERROR);
            bumpOut.setEnabled(false);
            danger.addValueChangeListener(event -> bumpOut.setEnabled(event.getValue()));
            result.add(danger, bumpOut);
            return result;
        })).setHeader("Akcje");
        playersGrid.setItems(gameService.getPlayers());
        playersGrid.getStyle().setMarginRight("1em");
        add(playersGrid);

    }
}
