package com.github.mjjaniec.lmq.views.maestro;

import static com.github.mjjaniec.lmq.util.TestId.testId;

import com.github.mjjaniec.lmq.components.DangerAction;
import com.github.mjjaniec.lmq.services.MaestroInterface;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RoutePrefix;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;

@Route(value = "")
@RoutePrefix(value = "maestro")
@RolesAllowed("MAESTRO")
@RequiredArgsConstructor
public class MaestroView extends VerticalLayout implements RouterLayout {

    private final MaestroInterface service;
    private final AuthenticationContext authenticationContext;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        DangerAction logout = new DangerAction("Wyloguj", _ -> authenticationContext.logout());
        testId(logout, "maestro/logout/danger", "maestro/logout/button");
        add(logout);
        UI ui = attachEvent.getUI();
        if (service.isGameStarted()) {
            ui.navigate(DjView.class);
        } else {
            ui.navigate(StartGameView.class);
        }
    }
}
