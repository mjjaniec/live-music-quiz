package com.github.mjjaniec.views.bigscreen;

import com.github.mjjaniec.components.ResultsTable;
import com.github.mjjaniec.services.BroadcastAttach;
import com.github.mjjaniec.services.GameService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@Route(value = "wrap-up", layout = BigScreenView.class)
public class WrapUpView extends VerticalLayout implements BigScreenRoute {

    private final BroadcastAttach broadcastAttach;
    private final GameService gameService;

    public WrapUpView(BroadcastAttach broadcastAttach, GameService gameService) {
        this.broadcastAttach = broadcastAttach;
        this.gameService = gameService;
        setSizeFull();
        setPadding(false);
        setSpacing(false);
    }

    void refresh() {
        gameService.stage().asWrapUp()
                .flatMap(w -> Optional.ofNullable(w.getDisplay()))
                .ifPresent(display -> {
                    removeAll();
                    if (display.table) {
                        add(new ResultsTable(gameService, display.showFrom));
                    } else {
                        add(new Paragraph(display.toString()));
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
