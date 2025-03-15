package com.github.mjjaniec.views.bigscreen;

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
@RequiredArgsConstructor
public class WrapUpView extends VerticalLayout implements BigScreenRoute {

    private final BroadcastAttach broadcastAttach;
    private final GameService gameService;

    void refresh() {
        gameService.stage().asWrapUp()
                .flatMap(w -> Optional.ofNullable(w.getDisplay()))
                .ifPresent(display -> {
                    removeAll();
                    add(new Paragraph(display.name()));
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
