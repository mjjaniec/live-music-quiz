package com.github.mjjaniec.views.bigscreen;

import com.github.mjjaniec.services.GameService;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route(value = "wrap-up", layout = BigScreenView.class)
public class WrapUpView extends VerticalLayout implements BigScreenRoute {

    public WrapUpView(GameService gameService) {
        add(new H1("dzięki za udział w grze!! Tu może kiedyś narysuję podium ;)"));
        add(new H1("Live Music Quiz by Michał Janiec"));
    }
}
