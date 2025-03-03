package com.github.mjjaniec.views.maestro;

import com.github.mjjaniec.services.MaestroInterface;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RoutePrefix;
import com.vaadin.flow.router.RouterLayout;
import lombok.RequiredArgsConstructor;

@Route(value = "")
@RoutePrefix(value = "maestro")
@RequiredArgsConstructor
public class MaestroView extends VerticalLayout implements RouterLayout {

    private final MaestroInterface service;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
//        UI ui = attachEvent.getUI();
//        if (service.isGameStarted()) {
//            ui.navigate(DjView.class);
//        } else {
//            ui.navigate(StartGameView.class);
//        }
    }
}
