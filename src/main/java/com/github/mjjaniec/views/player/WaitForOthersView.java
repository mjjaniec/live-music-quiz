package com.github.mjjaniec.views.player;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route(value = "wait-for-others", layout = PlayerView.class)
public class WaitForOthersView  extends VerticalLayout implements PlayerRoute {
    public WaitForOthersView() {
        add(new Span("poczekaj na pozostałych graczy"));
    }
}
