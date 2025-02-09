package com.github.mjjaniec.views.player;

import com.github.mjjaniec.services.GameService;
import com.github.mjjaniec.util.Cookies;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;
import lombok.RequiredArgsConstructor;

@Route("/")
@RequiredArgsConstructor
public class RootView extends HorizontalLayout implements PlayerRoute {

    private final GameService gameService;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        Cookies.readPlayer().ifPresentOrElse(
                player -> {
                    if (gameService.hasPlayer(player)) {
                        ui.navigate(WaitView.class);
                    } else {
                        Cookies.removePlayer();
                        ui.navigate(JoinView.class);
                    }
                },
                () -> ui.navigate(JoinView.class));
    }
}
