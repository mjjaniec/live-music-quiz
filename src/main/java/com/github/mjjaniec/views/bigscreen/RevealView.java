package com.github.mjjaniec.views.bigscreen;

import com.github.mjjaniec.components.NotesAnimation;
import com.github.mjjaniec.components.ProgressBar;
import com.github.mjjaniec.services.GameService;
import com.github.mjjaniec.util.Palette;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route(value = "reveal", layout = BigScreenView.class)
public class RevealView extends VerticalLayout implements BigScreenRoute {

    public RevealView(GameService gameService) {
        setSpacing(true);
        setPadding(true);
        getThemeList().add("spacing-l");


        setSizeFull();
        setAlignItems(Alignment.CENTER);
        add(new Div());
        add(new H1("Czekamy na"));
        add(new VerticalLayout());
    }
}
