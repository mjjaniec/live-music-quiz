package com.github.mjjaniec.lmq.views.bigscreen;

import com.github.mjjaniec.lmq.components.PodiumComponent;
import com.github.mjjaniec.lmq.components.ResultsTable;
import com.github.mjjaniec.lmq.services.Results;
import com.github.mjjaniec.lmq.services.BroadcastAttach;
import com.github.mjjaniec.lmq.services.GameService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.util.Optional;

@Route(value = "wrap-up", layout = BigScreenView.class)
public class WrapUpView extends VerticalLayout implements BigScreenRoute {

    private final BroadcastAttach broadcastAttach;
    private final GameService gameService;
    private final Results results;

    public WrapUpView(BroadcastAttach broadcastAttach, GameService gameService) {
        this.broadcastAttach = broadcastAttach;
        this.gameService = gameService;
        results = gameService.results();
        setSizeFull();
        setPadding(false);
        setSpacing(false);
    }

    void refresh() {
        gameService.wrapUpStage()
                .flatMap(w -> Optional.ofNullable(w.getShowFrom()))
                .ifPresent(showFrom -> {
                    removeAll();
                    if (1 <= showFrom && showFrom <= 3) {
                        add(new PodiumComponent(results, showFrom));
                    } else {
                        add(new ResultsTable(results, showFrom, true));
                    }
                });
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        broadcastAttach.attachWrapUp(attachEvent.getUI(), this::refresh);
        refresh();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcastAttach.detachWrapUp(detachEvent.getUI());
        super.onDetach(detachEvent);
    }
}
