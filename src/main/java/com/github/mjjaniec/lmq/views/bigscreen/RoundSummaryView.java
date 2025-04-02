package com.github.mjjaniec.lmq.views.bigscreen;

import com.github.mjjaniec.lmq.components.ResultsTable;
import com.github.mjjaniec.lmq.services.GameService;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route(value = "round-summary", layout = BigScreenView.class)
public class RoundSummaryView extends VerticalLayout implements BigScreenRoute {

    public RoundSummaryView(GameService gameService) {
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        add(new ResultsTable(gameService.results(), 0));
    }
}
